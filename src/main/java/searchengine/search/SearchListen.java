package searchengine.search;
import org.springframework.stereotype.Component;
import searchengine.response.SearchRes;

import java.util.concurrent.SynchronousQueue;
@Component
public class SearchListen implements Runnable {
    private static final SynchronousQueue<SearchRequest> requestQueue = new SynchronousQueue();
    private static final SynchronousQueue<SearchRes> responseQueue = new SynchronousQueue();

    public static SynchronousQueue<SearchRequest> getRequestQueue() {
        return requestQueue;
    }

    public static SynchronousQueue<SearchRes> getResponseQueue() {
        return responseQueue;
    }

    public SearchListen() {
        new Thread(this, "SearchListen").start();
    }
    @Override
    public void run() {
        for (; ; ) {
            SearchRequest request;
            try {
                request = requestQueue.take();
                SearchRes response = ResponseBuilder.receiveResponse(request);
                responseQueue.put(response);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}