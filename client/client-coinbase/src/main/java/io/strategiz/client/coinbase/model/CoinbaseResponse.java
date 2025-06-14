package io.strategiz.client.coinbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;

/**
 * Generic response wrapper for Coinbase API responses
 * @param <T> Type of data in the response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinbaseResponse<T> {
    private List<T> data;
    private Pagination pagination;
    
    public CoinbaseResponse() {
    }
    
    public CoinbaseResponse(List<T> data, Pagination pagination) {
        this.data = data;
        this.pagination = pagination;
    }
    
    public List<T> getData() {
        return data;
    }
    
    public void setData(List<T> data) {
        this.data = data;
    }
    
    public Pagination getPagination() {
        return pagination;
    }
    
    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoinbaseResponse<?> that = (CoinbaseResponse<?>) o;
        return Objects.equals(data, that.data) && 
               Objects.equals(pagination, that.pagination);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(data, pagination);
    }
    
    @Override
    public String toString() {
        return "CoinbaseResponse{" +
                "data=" + data +
                ", pagination=" + pagination +
                '}';
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        private String ending_before;
        private String starting_after;
        private String previous_uri;
        private String next_uri;
        private int limit;
        private String order;
        
        public Pagination() {
        }
        
        public Pagination(String ending_before, String starting_after, String previous_uri,
                         String next_uri, int limit, String order) {
            this.ending_before = ending_before;
            this.starting_after = starting_after;
            this.previous_uri = previous_uri;
            this.next_uri = next_uri;
            this.limit = limit;
            this.order = order;
        }
        
        public String getEnding_before() {
            return ending_before;
        }
        
        public void setEnding_before(String ending_before) {
            this.ending_before = ending_before;
        }
        
        public String getStarting_after() {
            return starting_after;
        }
        
        public void setStarting_after(String starting_after) {
            this.starting_after = starting_after;
        }
        
        public String getPrevious_uri() {
            return previous_uri;
        }
        
        public void setPrevious_uri(String previous_uri) {
            this.previous_uri = previous_uri;
        }
        
        public String getNext_uri() {
            return next_uri;
        }
        
        public void setNext_uri(String next_uri) {
            this.next_uri = next_uri;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public void setLimit(int limit) {
            this.limit = limit;
        }
        
        public String getOrder() {
            return order;
        }
        
        public void setOrder(String order) {
            this.order = order;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pagination that = (Pagination) o;
            return limit == that.limit &&
                   Objects.equals(ending_before, that.ending_before) &&
                   Objects.equals(starting_after, that.starting_after) &&
                   Objects.equals(previous_uri, that.previous_uri) &&
                   Objects.equals(next_uri, that.next_uri) &&
                   Objects.equals(order, that.order);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(ending_before, starting_after, previous_uri, next_uri, limit, order);
        }
        
        @Override
        public String toString() {
            return "Pagination{" +
                   "ending_before='" + ending_before + '\'' +
                   ", starting_after='" + starting_after + '\'' +
                   ", previous_uri='" + previous_uri + '\'' +
                   ", next_uri='" + next_uri + '\'' +
                   ", limit=" + limit +
                   ", order='" + order + '\'' +
                   '}';
        }
    }
}
