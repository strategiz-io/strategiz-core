package io.strategiz.auth.response;

import lombok.Builder;
import lombok.Data;

/**
 * Generic API response wrapper
 * @param <T> The type of data in the response
 */
@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
