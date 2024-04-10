package searchengine.dto.statistics;

import lombok.Data;

@Data
public class PageData {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    Float relevance;
}
