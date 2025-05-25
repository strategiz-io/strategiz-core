package strategiz.data.exchange.binanceus.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

/**
 * Response model for account balance information
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountBalanceResponse {
    private List<Balance> positions;
    private double totalUsdValue;
    private Account rawAccountData; // Complete unmodified raw data from the API
}
