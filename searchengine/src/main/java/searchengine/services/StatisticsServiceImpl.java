package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteStatus;
import searchengine.model.SiteTable;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;


@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private SitesList sites;
    @Autowired
    IndexingService indexingService;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private PageRepository pageRepository;
    private HashMap<searchengine.config.Site, Integer> pageCount = new HashMap<>();
    private HashMap<searchengine.config.Site, Long> statusTime = new HashMap<>();
    private HashMap<searchengine.config.Site, String> statuses = new HashMap<>();
    private HashMap<searchengine.config.Site, String> errors = new HashMap<>();
    private HashMap<searchengine.config.Site, Integer> lemmas = new HashMap<>();
    private final Logger logger = LogManager.getRootLogger();

    private void setSiteStatus() {
        for (searchengine.config.Site site : sites.getSites()) {
            Optional<SiteTable> siteMOptional = siteRepository.findByUrl(site.getUrl());
            if (siteMOptional.isEmpty()) {
                pageCount.put(site, 0);
                statusTime.put(site, 0L);
                statuses.put(site, "FAILED");
                errors.put(site, "Запись не найдена");
                lemmas.put(site,0);
                continue;
            }
            SiteTable siteM = siteMOptional.get();
            Integer siteId = siteM.getId();

            lemmas.put(site,lemmaRepository.countLemmasBySiteId(siteId));
            pageCount.put(site, pageRepository.countPagesBySiteId(siteId));
            statuses.put(site, siteM.getStatus().toString());
            statusTime.put(site, siteM.getStatusTime().getTime());
            errors.put(site, siteM.getLastError());
        }
    }

    @Override
    public StatisticsResponse getStatistics() {
        checkIfIndexingWasInterrupted();
        setSiteStatus();
        String[] statuses = new String[sites.getSites().size()];
        String[] errors = new String[sites.getSites().size()];

        for (int i = 0; i < statuses.length; i++) {
            statuses[i] = (this.statuses.get(sites.getSites().get(i)));
            errors[i] = (this.errors.get(sites.getSites().get(i)));

        }

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<searchengine.config.Site> sitesList = sites.getSites();
        for (int i = 0; i < sitesList.size(); i++) {
            searchengine.config.Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = pageCount.get(site);
            int lemmas = this.lemmas.get(site);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(statuses[i % sites.getSites().size()]);
            item.setError(errors[i % sites.getSites().size()]);
            item.setStatusTime(statusTime.get(site));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private void checkIfIndexingWasInterrupted() {
        for (searchengine.config.Site site : sites.getSites()) {
            Optional<SiteTable> siteMOptional = siteRepository.findByUrl(site.getUrl());
            if (siteMOptional.isEmpty()) {
                continue;
            }

            SiteTable siteToCheck = siteMOptional.get();

            if (siteToCheck.getStatus().equals(SiteStatus.INDEXING) & !indexingService.getIndexingStatus()) {
                siteToCheck.setStatus(SiteStatus.FAILED);
                siteToCheck.setLastError("Индексация завершена некорректно");
                logger.info("Индексация завершена некорректно");
            }
        }
    }
}

