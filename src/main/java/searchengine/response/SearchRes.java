package searchengine.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class SearchRes extends Response {
    private int count;
    private List<PageData> data = new ArrayList<>();
}
