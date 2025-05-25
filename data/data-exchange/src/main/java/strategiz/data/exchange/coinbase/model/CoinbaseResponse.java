package strategiz.data.exchange.coinbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * Generic response wrapper for Coinbase API responses
 * @param <T> Type of data in the response
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinbaseResponse<T> {
    private List<T> data;
    private Pagination pagination;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        private String ending_before;
        private String starting_after;
        private String previous_uri;
        private String next_uri;
        private int limit;
        private String order;
    }
}
