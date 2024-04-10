package searchengine.services;

import searchengine.dto.statistics.SearchResponse;

public interface SearchingService {
    SearchResponse getSearchResponse(String query, String site, Integer offset, Integer limit);
}
