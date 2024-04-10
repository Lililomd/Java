package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingPageResponse;
import searchengine.dto.statistics.IndexingStartingResponse;
import searchengine.dto.statistics.IndexingStoppingResponse;
import searchengine.model.SiteStatus;
import searchengine.model.SiteTable;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.scanner.LemmasAndIndexesManager;
import searchengine.scanner.PageScanner;
import searchengine.scanner.SiteScanner;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


@Transactional
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final Logger logger = LogManager.getRootLogger();
    @Autowired
    PageScanner pageScanner;
    private final SitesList sites;
    private ArrayList<SiteTable> sitesM;
    @Autowired
    private LemmasAndIndexesManager lemmasAndIndexesManager;
    private boolean indexingInProcess = false;
    @Override
    public Boolean getIndexingStatus() {
        return indexingInProcess;
    }
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;


    @Override
    public IndexingStoppingResponse getStopResponse() {
        IndexingStoppingResponse response = new IndexingStoppingResponse();
        response.setResult(indexingInProcess);
        response.setError(!indexingInProcess ? "Индексация не запущена" : null);

        if (indexingInProcess) {
            indexingInProcess = false;
            SiteScanner.stopScanning();
        }
        return response;
    }
    @Override
    public IndexingStartingResponse getStartResponse() {
        IndexingStartingResponse response = new IndexingStartingResponse();
        response.setResult(!indexingInProcess);
        response.setError(indexingInProcess ? "Индексация уже запущена" : null);
        if (!indexingInProcess) {
            indexingInProcess = true;
            logger.info("Индексация запущена");
            new Thread(this::startIndexing).start();
        }
        return response;
    }

    @Override
    public IndexingPageResponse getIndexingPageResponse(String url) {
        IndexingPageResponse response = new IndexingPageResponse();
        URL recievedURL;
        try {
            recievedURL = new URL(url);
        } catch (MalformedURLException e) {
            response.setError("некорректная ссылка");
            logger.info("некорректная ссылка");
            response.setResult(false);
            return response;
        }
        for (searchengine.config.Site site : sites.getSites()) {
            URL siteURL;
            try {siteURL = new URL(site.getUrl());}
            catch (MalformedURLException e) {
                response.setError("некорректная ссылка в файле \"application.yaml\"");
                logger.info("Некорректная ссылка в файле \"application.yaml\"");
                response.setResult(false);
                return response;
            }

            String host1 = siteURL.getHost().startsWith("www.") ? siteURL.getHost().substring(4) : siteURL.getHost();
            String host2 = recievedURL.getHost().startsWith("www.") ? recievedURL.getHost().substring(4) : recievedURL.getHost();

            if (host1.equals(host2)) {
                pageScanner.scanPage(recievedURL,site.getName());
                response.setResult(true);
                response.setError(null);
                return response;
            }
        }

        response.setResult(false);
        response.setError("Данная страница находится за пределами сайтов,\n" +
                "указанных в конфигурационном файле");
        logger.info("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        return response;
    }


    private void startIndexing() {
        sitesM = new ArrayList<>();
        List<String> siteUrls = sites.getSites().stream()
                .map(searchengine.config.Site::getUrl)
                .toList();

        deleteSitesAndPages(siteUrls);
        createSites(sites);

        for (SiteTable site : sitesM) {
            new Thread(() -> {
                try {
                    new SiteScanner(siteRepository, pageRepository, site, lemmasAndIndexesManager).scan();
                } catch (MalformedURLException e) {
                    logger.error(e.getMessage());
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private void deleteSitesAndPages(List<String> siteUrls) {
        for (String siteUrl : siteUrls) {
            Optional<SiteTable> siteM = siteRepository.findByUrl(siteUrl);
            if (siteM.isEmpty()) {
                continue;
            }
            lemmasAndIndexesManager.clearIndexesAndLemmasBySite(siteM.get());
        }
        pageRepository.deleteBySite_UrlIn(siteUrls);
        siteRepository.deleteByUrlIn(siteUrls);
    }

    private void createSites(SitesList list) {
        List<searchengine.config.Site> sites = list.getSites();
        for (searchengine.config.Site site : sites) {
            SiteTable siteM = new SiteTable();
            siteM.setUrl(site.getUrl());
            siteM.setName(site.getName());
            siteM.setStatus(SiteStatus.FAILED);
            siteM.setLastError("Индексация не была запущена");
            siteM.setStatusTime(new Date(System.currentTimeMillis()));
            sitesM.add(siteM);
            siteRepository.save((siteM));
            siteRepository.flush();
        }
    }
}
