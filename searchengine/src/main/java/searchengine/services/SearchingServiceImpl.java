package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.PageData;
import searchengine.dto.statistics.SearchResponse;
import searchengine.model.Page;
import searchengine.model.SiteTable;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.scanner.LemmaFinder;

import java.io.IOException;
import java.util.*;

@Service
public class SearchingServiceImpl implements SearchingService {
    private static final int maxFrequencyPersentage = 90;
    @Autowired
    private SitesList sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private IndexRepository indexRepository;
    private List<String> lemmasUsedInSnippetMax;
    private final Logger logger = LogManager.getRootLogger();
    LemmaFinder lemmaFinder;

    {
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchResponse getSearchResponse(String query, String site, Integer offset, Integer limit) {

        List<String> lemmasFromQuery = getLemmas(query);
        List<Integer> idsOfSitesToSearchIn = getSitesIds(site);
        List<String> sortedAndFilteredLemmas = sortAndFilter(lemmasFromQuery, idsOfSitesToSearchIn);
        if (sortedAndFilteredLemmas.size() == 0) {
            return errorResponse();
        }
        List<Integer> filteredPageIds = getRelevantPagesIds(sortedAndFilteredLemmas, idsOfSitesToSearchIn);
        Map<Integer, Integer> pageToRank = getPagesRanks(filteredPageIds, sortedAndFilteredLemmas);
        Integer count = filteredPageIds.size();
        List<Integer> sortedAndFilteredPages = getSortedAndLimitedPages(pageToRank, limit);
        Map<Integer, Float> pageToRelRank = absoluteToRelevant(pageToRank);
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setCount(count);
        List<PageData> pageDataList = new ArrayList<>();
        for (Integer pageId : sortedAndFilteredPages) {
            PageData pageData = new PageData();
            Page page = pageRepository.findById(pageId).get();
            pageData.setUri(page.getPath());
            pageData.setSite(page.getSite().getUrl());
            pageData.setSiteName(page.getSite().getName());
            pageData.setRelevance(pageToRelRank.get(pageId));
            pageData.setTitle(getPageTitle(page));
            pageData.setSnippet(getSnippet(page, sortedAndFilteredLemmas));
            pageDataList.add(pageData);
        }
        response.setData(pageDataList);
        return response;
    }

    private SearchResponse errorResponse() {
        SearchResponse response = new SearchResponse();
        response.setError("Задан пустой поисковый запрос");
        response.setResult(false);
        return response;
    }

    private String getSnippetWithMostAmountOfLemmas(List<String> words, List<String> lemmas, LuceneMorphology morphology) {
        List<String> localWords = new ArrayList<>(words);
        int maxDistance = 3;
        int currDistance = 0;
        int maxCount = 0;
        int maxStart = 0;
        int end = 0;
        int count = 0;
        int start = 0;
        List<String> lemmasUsedInSnippet = new ArrayList<>();
        lemmasUsedInSnippetMax = new ArrayList<>();

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i).replaceAll("[\"'«»!?,.;:-]*\\s*$", "");
            List<String> lemmasFromWord;
            try {
                lemmasFromWord = morphology.getNormalForms(word.toLowerCase());
            } catch (Exception e) {
                logger.error(e.getMessage());
                continue;
            }

            String intersection = getIntersection(lemmasFromWord, lemmas);
            if (intersection != null) {
                String highlighted = "<b>" + words.get(i) + "</b>";
                localWords.set(i, highlighted);
                if (currDistance <= maxDistance) {
                    count++;
                } else {
                    lemmasUsedInSnippet.clear();
                    start = i;
                    count = 1;
                }
                lemmasUsedInSnippet.add(intersection);
                currDistance = 0;
                if (count > maxCount) {
                    lemmasUsedInSnippetMax = new ArrayList<>(lemmasUsedInSnippet);
                    maxCount = count;
                    maxStart = start;
                    end = i;
                }
            } else {
                currDistance++;
            }
        }

        start = (maxStart > 3 ? maxStart - 3 : 0);
        end = (end < localWords.size() - 3 ? end + 3 : localWords.size());
        List<String> result = localWords.subList(start, end);
        String joinedString = String.join(" ", result);
        joinedString = joinedString.trim();
        return joinedString;
    }

    private String getSnippet(Page page, List<String> lemmas) {
        LuceneMorphology morphology;
        try {
            morphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }


        Document doc = Jsoup.parse(page.getContent());
        String text = doc.text();
        List<String> words = Arrays.stream(text.split("\\s+")).toList();

        String firstSnippet = getSnippetWithMostAmountOfLemmas(words, lemmas, morphology);
        List<String> lemmasWasNotUsed = getNotUsedLemmas(lemmas);
        List<String> snippets = new ArrayList<>();
        snippets.add(firstSnippet);
        if (lemmasWasNotUsed.isEmpty()) {
            return formResultSnippet(snippets);
        }

        String secondSnippet = getSnippetWithMostAmountOfLemmas(words, lemmasWasNotUsed, morphology);
        lemmasWasNotUsed = getNotUsedLemmas(lemmasWasNotUsed);
        snippets.add(secondSnippet);
        if (lemmasWasNotUsed.isEmpty()) {
            return formResultSnippet(snippets);
        }

        String thirdSnippet = getSnippetWithMostAmountOfLemmas(words, lemmasWasNotUsed, morphology);

        snippets.add(thirdSnippet);
        return formResultSnippet(snippets);


    }

    public String formResultSnippet(List<String> strings) {
        String result = String.join(" ... ", strings);
        String[] words = result.split(" ");
        StringBuilder sb = new StringBuilder();
        int wordCount = 0;
        for (String word : words) {
            sb.append(word).append(" ");
            wordCount++;
            if (wordCount >= 10) {
                sb.append("<br>");
                wordCount = 0;
            }
        }
        return sb.toString();
    }

    private List<String> getNotUsedLemmas(List<String> allLemmas) {
        List<String> notUsedLemmas = new ArrayList<>();
        for (String lemma : allLemmas) {
            if (!lemmasUsedInSnippetMax.contains(lemma)) {
                notUsedLemmas.add(lemma);
            }
        }
        return notUsedLemmas;
    }

    private String getIntersection(List<String> list1, List<String> list2) {
        for (String s1 : list1) {
            for (String s2 : list2) {
                if (s1.equals(s2)) {
                    return s2;
                }
            }
        }
        return null;
    }

    private String getPageTitle(Page page) {
        Document doc = Jsoup.parse(page.getContent());
        return doc.title();
    }

    private Map<Integer, Float> absoluteToRelevant(Map<Integer, Integer> pageToRank) {
        int maxRank = Collections.max(pageToRank.values());

        Map<Integer, Float> relevantRanks = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : pageToRank.entrySet()) {
            int absoluteRank = entry.getValue();
            float relevantRank = (float) absoluteRank / maxRank;
            relevantRanks.put(entry.getKey(), relevantRank);
        }

        return relevantRanks;
    }

    private List<Integer> getSortedAndLimitedPages(Map<Integer, Integer> pagesAndRanks, Integer limit) {
        List<Map.Entry<Integer, Integer>> list = new LinkedList<>(pagesAndRanks.entrySet());
        list.sort(Map.Entry.<Integer, Integer>comparingByValue().reversed());

        List<Integer> sortedPageIds = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : list) {
            sortedPageIds.add(entry.getKey());
        }
        sortedPageIds = sortedPageIds.subList(0, Math.min(limit, sortedPageIds.size()));
        return sortedPageIds;
    }

    private Map<Integer, Integer> getPagesRanks(List<Integer> pageIds, List<String> lemmas) {
        HashMap<Integer, Integer> ranks = new HashMap<>();
        for (Integer pageId : pageIds) {
            Integer pageRank = 0;
            for (String lemma : lemmas) {
                List<Integer> lemmaIds = lemmaRepository.findIdsByLemma(lemma);
                Integer rank = indexRepository.findPageIdsByLemmaIdsAndPageId(lemmaIds, pageId);
                pageRank += rank;
            }
            ranks.put(pageId, pageRank);

        }
        return ranks;
    }

    private List<Integer> getRelevantPagesIds(List<String> sortedAndFilteredLemmas, List<Integer> idsOfSitesToSearchIn) {
        List<String> localSortedAndFilteredLemmas = new ArrayList<>(sortedAndFilteredLemmas);
        String firstLemma = localSortedAndFilteredLemmas.remove(0);
        List<Integer> lemmaIds = lemmaRepository.findIdsByLemmaAndSiteIds(firstLemma, idsOfSitesToSearchIn);
        List<Integer> pagesId = indexRepository.findPageIdsByLemmaIds(lemmaIds);

        for (String lemma : localSortedAndFilteredLemmas) {
            lemmaIds = lemmaRepository.findIdsByLemmaAndSiteIds(lemma, idsOfSitesToSearchIn);
            List<Integer> filteredPageIds = indexRepository.findPageIdsByLemmaIdsAndPageIds(lemmaIds, pagesId);
            pagesId.clear();
            pagesId.addAll(filteredPageIds);
        }
        return pagesId;
    }

    private List<String> sortAndFilter(List<String> lemmas, List<Integer> siteIds) {
        Integer pageCount = getPageCount(siteIds);

        HashMap<String, Integer> lemmasFrequency = new HashMap<>();
        for (String lemma : lemmas) {
            Integer frequency = lemmaRepository.getFrequencySum(lemma, siteIds);
            if (frequency == null) {
                continue;
            }
            if (frequency > pageCount * maxFrequencyPersentage / 100) {
                continue;
            }
            lemmasFrequency.put(lemma, frequency);
        }
        List<Map.Entry<String, Integer>> list = new LinkedList<>(lemmasFrequency.entrySet());
        list.sort(Map.Entry.comparingByValue());

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : list) {
            result.add(entry.getKey());
        }
        return result;
    }


    private Integer getPageCount(List<Integer> sitesId) {
        Integer result = 0;
        for (Integer siteId : sitesId) {
            result += pageRepository.countPagesBySiteId(siteId);
        }
        return result;
    }

    private List<String> getLemmas(String line) {
        ArrayList<String> lemmas = new ArrayList<>(lemmaFinder.getLemmas(line).keySet());
        return lemmas;
    }

    private List<Integer> getSitesIds(String siteFromQuery) {
        if (siteFromQuery == null) {
            return getAllSites();
        }

        Optional<SiteTable> siteM = siteRepository.findByUrl(siteFromQuery);
        ArrayList<Integer> result = new ArrayList<>();
        if (siteM.isPresent()) {
            result.add(siteM.get().getId());
            return result;
        }
        return null;
    }

    private List<Integer> getAllSites() {
        List<Integer> ids = new ArrayList<>();
        for (searchengine.config.Site site : sites.getSites()) {
            Optional<SiteTable> siteM = siteRepository.findByUrl(site.getUrl());
            if (siteM.isEmpty()) {
                continue;
            }
            ids.add(siteM.get().getId());
        }
        return ids;
    }
}
