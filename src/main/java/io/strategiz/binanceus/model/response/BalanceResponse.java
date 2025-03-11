package io.strategiz.binanceus.model.response;

import io.strategiz.framework.rest.model.BaseServiceResponse;
import io.strategiz.binanceus.model.Account;
import io.strategiz.binanceus.model.Balance;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class BalanceResponse extends BaseServiceResponse {
    private List<Balance> positions;
    private double totalUsdValue;
    private Account rawAccountData; // Complete unmodified raw data from the API
}
