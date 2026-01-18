package io.strategiz.service.cryptotoken.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for purchasing STRAT tokens with USD.
 */
public record PurchaseTokensRequest(@NotNull @Min(500) Long amountInCents // Minimum $5.00 = 500 cents
) {

}
