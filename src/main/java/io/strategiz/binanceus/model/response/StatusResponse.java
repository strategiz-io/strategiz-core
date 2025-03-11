package io.strategiz.binanceus.model.response;

import io.strategiz.framework.rest.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class StatusResponse extends BaseServiceResponse {
    private String status;
    private String message;
    private Long timestamp;
    
    public StatusResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public StatusResponse(String status, String message) {
        this();
        this.status = status;
        this.message = message;
    }
    
    public static StatusResponse success(String message) {
        return new StatusResponse("success", message);
    }
    
    public static StatusResponse error(String message) {
        return new StatusResponse("error", message);
    }
}
