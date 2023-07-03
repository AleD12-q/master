package searchengine.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import searchengine.controllers.Response;

@Data
@EqualsAndHashCode(callSuper = false)
public class ErrorResponse extends Response {
    private String error;

    public ErrorResponse(String error) {
        setResult(false);
        this.error = error;
    }
}
