package searchengine.services;

import searchengine.dto.statistics.IndexingPageResponse;
import searchengine.dto.statistics.IndexingStartingResponse;
import searchengine.dto.statistics.IndexingStoppingResponse;

public interface IndexingService {
    IndexingStoppingResponse getStopResponse();
    IndexingStartingResponse getStartResponse();
    IndexingPageResponse getIndexingPageResponse(String url);
    Boolean getIndexingStatus();
}
