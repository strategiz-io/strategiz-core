package io.strategiz.service.cryptotoken.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for transferring STRAT tokens to another user.
 */
public record TransferTokensRequest(@NotBlank String recipientUserId, @NotNull @Min(1) Long amount, // In micro-units
		String description) {

}
