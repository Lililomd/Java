package searchengine.scanner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.SiteStatus;
import searchengine.model.SiteTable;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

public class SiteScanner {
    private static boolean scanningIsStopped = false;
    public static void stopScanning(){scanningIsStopped = true;}
//    private long start = System.currentTimeMillis();
    SiteAccessController siteAccessController;

    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private SiteTable site;
    private LemmasAndIndexesManager lemmasAndIndexesManager;
    private URL startUrl;
    private final Logger logger = LogManager.getRootLogger();

    private final int maxDepth = 10;
    int depth = 0;
    private ConcurrentLinkedQueue<URL> queueToWrite = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<URL> queueToScan = new ConcurrentLinkedQueue<>();
    private Set<URL> markedPages = new HashSet<>();
    ExecutorService executor;
    int linkCountOnCurrentDepth;


    public SiteScanner(SiteRepository siteRepository, PageRepository pageRepository, SiteTable site, LemmasAndIndexesManager lemmasAndIndexesManager) throws MalformedURLException {
        this.lemmasAndIndexesManager = lemmasAndIndexesManager;
        siteRepository.updateErrorById(site.getId(), "");
        siteRepository.updateStatusById(site.getId(), SiteStatus.INDEXING);
        siteRepository.updateStatusTimeById(site.getId(), new Date(System.currentTimeMillis()));
        siteAccessController = new SiteAccessController(site, pageRepository);
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.site = site;
        startUrl = new URL(site.getUrl());
    }

    public void scan(){
        scanningIsStopped = false;
        queueToScan.offer(startUrl);
        int maxDepth = 10;
        while (!queueToScan.isEmpty() || depth >= maxDepth) {
            executor = Executors.newFixedThreadPool(6);
            linkCountOnCurrentDepth = queueToScan.size();
            while (!queueToScan.isEmpty()) {

                executor.execute(() -> extractLinks(queueToScan.poll()));
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
            if(scanningIsStopped){
                siteRepository.updateStatusById(site.getId(), SiteStatus.FAILED);
                siteRepository.updateErrorById(site.getId(), "индексация прервана пользователем");
                logger.info("Индексация прервана пользователем");
                break;
            }
            executor.shutdown();
            while (!executor.isTerminated()){}

            queueToScan.addAll(queueToWrite);
            queueToWrite.clear();
            depth++;
        }
        executor.shutdown();
        if(scanningIsStopped){return;}
        siteRepository.updateStatusById(site.getId(), SiteStatus.INDEXED);
        logger.info("Индексация сайта " + site.getName() + " окончена");

    }


    private void extractLinks(URL processedUrl) {
        if(scanningIsStopped){return;}

        String pageContent;
        try {pageContent = siteAccessController.accessSite(processedUrl);}
        catch (InterruptedException | IOException e) {return;}

        if(pageContent == null){return;}
        Page page = new Page();
        page.setSite(site);
        page.setPath(processedUrl.getPath());
        page.setCode(200);
        page.setContent(pageContent);

        if (!checkAndSave(page)) {return;}
        lemmasAndIndexesManager.handlePageContent(page, pageContent);
        Document document = Jsoup.parse(pageContent);
        System.gc();
        Elements elements = document.select("a[href]");
        for (Element element : elements) {
            String link = element.attr("href");
            if (link.contains("#") || link.endsWith("pdf") || link.endsWith("jpg")){
                continue;
            }
            if (link.endsWith("/")) {
                link = link.substring(0, link.length() - 1);
            }
            URL absoluteURL;
            try {
                absoluteURL = new URL(processedUrl, link);
            } catch (MalformedURLException e) {
                logger.error(e.getMessage());
                continue;
            }



            if (!absoluteURL.getHost().equals(startUrl.getHost())) {
                continue;
            }

            synchronized (markedPages) {
                if (markedPages.contains(absoluteURL)) {
                    continue;
                }
                markedPages.add(absoluteURL);
            }

            queueToWrite.add(absoluteURL);
        }
    }


    private boolean checkAndSave(Page page) {
        if (pageRepository.existsByPathAndSiteId(page.getPath(), site.getId())){
            return false;
        }
        pageRepository.save(page);
        siteRepository.updateStatusTimeById(site.getId(), new Date(System.currentTimeMillis()));
        return true;
    }
}

