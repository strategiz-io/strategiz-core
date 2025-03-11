package io.strategiz.binanceus.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountBalanceResponse {
    private List<Balance> positions;
    private double totalUsdValue;
    private Account rawAccountData; // Complete unmodified raw data from the API
}
