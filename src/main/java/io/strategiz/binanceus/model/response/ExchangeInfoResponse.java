package io.strategiz.binanceus.model.response;

import io.strategiz.framework.rest.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ExchangeInfoResponse extends BaseServiceResponse {
    private Object exchangeInfo;
}
