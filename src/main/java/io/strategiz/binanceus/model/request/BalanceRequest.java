package io.strategiz.binanceus.model.request;

import io.strategiz.framework.rest.model.BaseServiceRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class BalanceRequest extends BaseServiceRequest {
    private String apiKey;
    private String secretKey;
    private String userId;
}
