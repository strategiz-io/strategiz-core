package strategiz.data.exchange.binanceus.model.response;

import strategiz.data.exchange.binanceus.model.Account;
import strategiz.data.exchange.binanceus.model.Balance;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Response model for balance information
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BalanceResponse {
    private List<Balance> positions;
    private double totalUsdValue;
    private Account rawAccountData; // Complete unmodified raw data from the API
}
