package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.response.Response;
import searchengine.search.SearchListen;
import searchengine.search.SearchRequest;
import searchengine.response.ErrorResponse;
import searchengine.response.SearchRes;

@Slf4j
@RestController
public class SearchController {
    @GetMapping("/search")
    public Response search(@RequestParam(required = false) String query,
                           @RequestParam(required = false) String site,
                           @RequestParam(required = false) Integer offset,
                           @RequestParam(required = false) Integer limit) {
        log.info("Поисковый запрос: " + query);
        SearchRequest request = new SearchRequest().buildRequest(query, site, offset, limit);
        if (request == null) {
            return new ErrorResponse("Задан пустой поисковый запрос");
        }
        log.info("Леммы: " + request.getQueryWords());
        return receiveResponse(request);
    }

    private Response receiveResponse(SearchRequest request) {
        SearchRes response;
        try {
            SearchListen.getRequestQueue().put(request);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            response = SearchListen.getResponseQueue().take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
