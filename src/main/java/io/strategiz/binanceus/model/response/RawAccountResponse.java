package io.strategiz.binanceus.model.response;

import io.strategiz.framework.rest.model.BaseServiceResponse;
import io.strategiz.binanceus.model.Account;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class RawAccountResponse extends BaseServiceResponse {
    private Account account;
}
