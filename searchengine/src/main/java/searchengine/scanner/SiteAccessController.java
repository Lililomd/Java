package searchengine.scanner;

import searchengine.model.SiteTable;
import searchengine.repositories.PageRepository;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.Semaphore;



public class SiteAccessController {
    private final Semaphore semaphore;
    PageRepository pageRepository;
    SiteTable site;

    public SiteAccessController(SiteTable site, PageRepository pageRepository) {
        this.semaphore = new Semaphore(1, true);
        this.site = site;
        this.pageRepository = pageRepository;
    }

    public String accessSite(URL page) throws InterruptedException, IOException {
        if (page == null){return null;}
        semaphore.acquire();

        String content;
        try {
            content = new Scanner(page.openStream(), StandardCharsets.UTF_8).useDelimiter("\\A").next();

            Thread.sleep(200);
            return content;
        }finally {
            this.unlock();
        }
    }
    public void unlock(){
        semaphore.release();
    }
}
