package searchengine.searchFolder;

import lombok.extern.slf4j.Slf4j;
import searchengine.builders.PageRelevance;
import searchengine.model.Index;
import searchengine.model.Site;
import searchengine.repository.Repos;
import searchengine.model.Page;
import searchengine.response.PageData;
import searchengine.response.SearchRes;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ResponseBuilder implements Runnable {
    private static final Map<SearchRequest, SearchRes> responses = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final SearchRequest request;
    private final SearchRes response;
    private Site site = null;
    private final List<LemmaFrequency> lemmaFrequencies = new ArrayList<>();
    private Set<PageRelevance> relevanceSet;
    private final Map<Integer, PageRelevance> relevanceMap = new HashMap<>();
    private List<PageRelevance> relevanceList;
    private searchengine.searchFolder.LemmaGear LemmaGear;


    public ResponseBuilder(SearchRequest request) {
        this.request = request;
        response = responses.get(request);
    }

    @Override
    public void run() {
        defineLemmaFrequencies();
        definePageRelevances();
        calculateRelevances();
        prepareResponse();
        request.setReady(true);
    }

    public static SearchRes receiveResponse(SearchRequest request) {
        removeOldResponses();

        request.setLastTime(System.currentTimeMillis() / 1000);

        SearchRes response = responses.get(request);
        if (response == null) {
            response = new SearchRes();
            responses.put(request, response);
            Runnable builder = new ResponseBuilder(request);
            executor.execute(builder);
        } else {
            responses.put(request, response);
        }

        for (; ; ) {
            if (response.getCount() > 0) {
                if (request.getOffset() + request.getLimit() > response.getCount()) {
                    request.setLimit(response.getCount() - request.getOffset());
                }
            } else if (request.isReady()) {
                return response;
            }
            boolean cond = request.getOffset() + request.getLimit() <= response.getData().size();
            if (cond) {
                response = formPartialResponse(request, response);
                request.setReady(true);
                return response;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            response = responses.get(request);
        }
    }

    private static void removeOldResponses() {
        for (Map.Entry<SearchRequest, SearchRes> entry : responses.entrySet()) {
            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime - entry.getKey().getLastTime() > 90) {
                responses.remove(entry.getKey());
            }
        }
    }

    private static SearchRes formPartialResponse(SearchRequest request, SearchRes response) {
        SearchRes partialResponse = new SearchRes();
        partialResponse.setCount(response.getCount());
        for (int index = request.getOffset();
             index < request.getOffset() + request.getLimit(); index++) {
            partialResponse.getData().add(response.getData().get(index));
        }
        return partialResponse;
    }

    private void defineLemmaFrequencies() {
        if (request.getSiteUrls().size() == 1) {
            site = Repos.siteRepo.findByUrlAndType(request.getSiteUrls().get(0), Site.INDEXED)
                    .orElse(null);
            if (site == null) {
                return;
            }
        }
        for (String queryWord : request.getQueryWords()) {
            int frequency = site != null ?
                    Repos.lemmaRepo
                            .findFrequencyByTextLemmaAndSite(queryWord, site)
                            .orElse(0) :
                    Repos.lemmaRepo
                            .findFrequencyByTextLemma(queryWord)
                            .orElse(0);
            if (frequency == 0) {
                continue;
            }
            LemmaFrequency lemmaFrequency = new LemmaFrequency();
            lemmaFrequency.setLemma(queryWord);
            lemmaFrequency.setFrequency(frequency);
            lemmaFrequencies.add(lemmaFrequency);
        }
        if (lemmaFrequencies.size() != request.getQueryWords().size()) {
            lemmaFrequencies.clear();
            return;
        }
        lemmaFrequencies.sort((lf1, lf2) ->
                Float.compare(lf1.getFrequency(), lf2.getFrequency()));
    }

    private void definePageRelevances() {
        if (lemmaFrequencies.size() == 0) {
            return;
        }
        defineRelevancesForFirstLemma(lemmaFrequencies.get(0).getLemma());
        for (int i = 1; i < lemmaFrequencies.size(); i++) {
            defineRelevancesForNextLemmas(lemmaFrequencies.get(i).getLemma());
        }
    }

    private void defineRelevancesForFirstLemma(String lemma) {
        List<Index> indices = site != null ?
                Repos.indexRepo.findAllByTextLemmaAndSite(lemma, site) :
                Repos.indexRepo.findAllByTextLemma(lemma);
        for (Index index : indices) {
            PageRelevance relevance = relevanceMap.get(index.getPage().getId());
            if (relevance == null) {
                relevance = new PageRelevance();
                relevance.setPage(index.getPage());
                relevanceMap.put(index.getPage().getId(), relevance);
            }
            LemmaGear lemmaGear = new LemmaGear(lemma, index.getRank());
            relevance.getLemmaGears().add(LemmaGear);
        }
        relevanceSet = new HashSet<>(relevanceMap.values());
    }

    private void defineRelevancesForNextLemmas(String lemma) {
        Set<PageRelevance> lemmaRelevances = new HashSet<>();
        List<Index> indices = site != null ?
                Repos.indexRepo.findAllByTextLemmaAndSite(lemma, site) :
                Repos.indexRepo.findAllByTextLemma(lemma);
        for (Index index : indices) {
            PageRelevance relevance = relevanceMap.get(index.getPage().getId());
            if (!relevanceSet.contains(relevance)) {
                continue;
            }
            LemmaGear lemmaRank = new LemmaGear(lemma, index.getRank());
            relevance.getLemmaGears().add(lemmaRank);
            lemmaRelevances.add(relevance);
        }
        relevanceSet.retainAll(lemmaRelevances);
    }

    private void calculateRelevances() {
        if (relevanceSet == null) {
            return;
        }
        float maxRelevance = 0;
        for (PageRelevance relevance : relevanceSet) {
            float absRelevance = 0;
            for (LemmaGear lemmaGear : relevance.getLemmaGears()) {
                absRelevance += lemmaGear.getGear();
            }
            relevance.setAbsoluteRelevance(absRelevance);
            if (absRelevance > maxRelevance) {
                maxRelevance = absRelevance;
            }
        }

        for (PageRelevance relevance : relevanceSet) {
            relevance.setRelativeRelevance(
                    relevance.getAbsoluteRelevance() / maxRelevance);
        }

        relevanceList = new ArrayList<>(relevanceSet);
        relevanceList.sort((r1, r2) -> -Float.compare(
                r1.getRelativeRelevance(), r2.getRelativeRelevance()));
    }

    private void prepareResponse() {
        int count = relevanceList != null ? relevanceList.size() : 0;
        response.setCount(count);
        for (int pageDataNumber = 0; pageDataNumber < count; pageDataNumber++) {
            PageData data = new PageData();
            PageRelevance relevance = relevanceList.get(pageDataNumber);
            Page page = relevance.getPage();
            String snippet = new Snippet(page, request.getQueryWords()).formSnippet();
            Site site = page.getSite();
            data.setSite(site.getUrl());
            data.setSiteName(site.getName());
            data.setUri(page.getPath());
            data.setTitle(page.getTitle());
            data.setSnippet(snippet);
            data.setRelevance(relevance.getRelativeRelevance());
            log.debug("pageDataNumber: " + (pageDataNumber + 1));
            response.getData().add(data);
        }
    }
}
