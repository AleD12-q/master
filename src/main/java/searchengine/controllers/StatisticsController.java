package searchengine.controllers;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.builders.SiteBuilder;

@RestController
public class StatisticsController {
    @GetMapping("/statistics")
    public Response statistics() {
        return SiteBuilder.getStatistics();
    }
}
