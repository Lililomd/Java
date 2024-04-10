package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.*;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchingService searchingService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingStartingResponse> startIndexing() {
        IndexingStartingResponse response = indexingService.getStartResponse();
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(400).body(response);

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingStoppingResponse> stopIndexing() {
        IndexingStoppingResponse response = indexingService.getStopResponse();
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(400).body(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingPageResponse> indexPage(@RequestParam String url) {
        IndexingPageResponse response = indexingService.getIndexingPageResponse(url);
        if (response.isResult()) {
            return ResponseEntity.ok(response);
        }
        if (response.getError().equals("некорректная ссылка") | response.getError().equals("Данная страница находится за пределами сайтов,\n" +
                "указанных в конфигурационном файле")) {
            return ResponseEntity.status(400).body(response);
        }
        return ResponseEntity.status(500).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query, @RequestParam(required = false) String site, @RequestParam Integer offset, @RequestParam Integer limit) {
        SearchResponse response = searchingService.getSearchResponse(query, site, offset, limit);
        return response.isResult() ? ResponseEntity.ok(response) : ResponseEntity.status(400).body(response);
    }
}
