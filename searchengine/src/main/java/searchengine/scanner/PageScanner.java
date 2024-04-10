package searchengine.scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.SiteStatus;
import searchengine.model.SiteTable;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.Scanner;

@Service
public class PageScanner {
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    LemmasAndIndexesManager lemmasAndIndexesManager;
    String siteName;
    SiteTable site;
    Page page;
    @Autowired
    PageRepository pageRepository;
    private final Logger logger = LogManager.getRootLogger();

    public void scanPage(URL page, String siteName) {
        this.siteName = siteName;
        if (checkIfSiteExist(page)) {
            checkAndClearPage(page);
        } else {
            createSite(page.getProtocol() + "://" + page.getHost());
        }
        createPage(page.toString());
    }

    private Boolean checkIfSiteExist(URL page) {
        URL siteURL;
        for (SiteTable site : siteRepository.findAll()) {
            try {
                siteURL = new URL(site.getUrl());
            } catch (MalformedURLException e) {
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
            if (siteURL.getHost().equals(page.getHost())) {
                this.site = site;
                return true;
            }
        }
        return false;
    }

    private void checkAndClearPage(URL page) {
        Optional<Page> optionalPage = pageRepository.findByPath(page.getPath());
        if (optionalPage.isEmpty()) {
            return;
        }
        lemmasAndIndexesManager.deleteIndexesAndUpdateLemmas(optionalPage.get().getId());
        pageRepository.delete(optionalPage.get());
        pageRepository.flush();
    }

    private void createPage(String page) {
        URL pageURL;
        String pageContent;
        try {
            pageURL = new URL(page);
            pageContent = new Scanner(pageURL.openStream(), StandardCharsets.UTF_8).useDelimiter("\\A").next();
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
        Page pageM = new Page();
        pageM.setPath(pageURL.getPath());
        pageM.setSite(site);
        pageM.setCode(200);
        pageM.setContent(pageContent);
        pageRepository.save(pageM);
        this.page = pageM;

        lemmasAndIndexesManager.handlePageContent(pageM, pageContent);
    }

    private void createSite(String url) {
        SiteTable site = new SiteTable();
        site.setLastError("Индексация не проводилась");
        logger.info("Индексация не проводилась");
        site.setStatus(SiteStatus.FAILED);
        site.setUrl(url);
        site.setStatusTime(new Date(System.currentTimeMillis()));
        site.setName(siteName);
        siteRepository.save(site);
        this.site = site;
    }
}
