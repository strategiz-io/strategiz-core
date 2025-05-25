package strategiz.data.exchange.binanceus.model.response;

import strategiz.data.exchange.binanceus.model.Account;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Response model for raw account information
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RawAccountResponse {
    private Account account;
}
