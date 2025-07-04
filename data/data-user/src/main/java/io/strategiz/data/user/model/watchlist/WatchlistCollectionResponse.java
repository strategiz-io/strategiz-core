package io.strategiz.data.user.model.watchlist;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Response model for collections of watchlist items.
 * Contains a list of items and metadata about the collection.
 */
public class WatchlistCollectionResponse {
    
    private List<ReadWatchlistItemResponse> items;
    private Integer totalCount;
    private Integer activeCount;
    private Boolean isEmpty;

    // Constructors
    public WatchlistCollectionResponse() {
        this.items = new ArrayList<>();
    }

    public WatchlistCollectionResponse(List<ReadWatchlistItemResponse> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.totalCount = this.items.size();
        this.activeCount = (int) this.items.stream()
                .filter(item -> item.getIsActive() != null && item.getIsActive())
                .count();
        this.isEmpty = this.items.isEmpty();
    }

    /**
     * Creates a collection response from a list of MarketWatchlistItem entities.
     * 
     * @param entities The entities to convert
     * @return WatchlistCollectionResponse
     */
    public static WatchlistCollectionResponse fromEntities(List<MarketWatchlistItem> entities) {
        if (entities == null || entities.isEmpty()) {
            return new WatchlistCollectionResponse();
        }
        
        List<ReadWatchlistItemResponse> items = entities.stream()
                .map(ReadWatchlistItemResponse::fromEntity)
                .collect(Collectors.toList());
        
        return new WatchlistCollectionResponse(items);
    }

    // Getters and Setters
    public List<ReadWatchlistItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ReadWatchlistItemResponse> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.totalCount = this.items.size();
        this.activeCount = (int) this.items.stream()
                .filter(item -> item.getIsActive() != null && item.getIsActive())
                .count();
        this.isEmpty = this.items.isEmpty();
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(Integer activeCount) {
        this.activeCount = activeCount;
    }

    public Boolean getIsEmpty() {
        return isEmpty;
    }

    public void setIsEmpty(Boolean isEmpty) {
        this.isEmpty = isEmpty;
    }

    /**
     * Adds an item to the collection.
     * 
     * @param item The item to add
     */
    public void addItem(ReadWatchlistItemResponse item) {
        if (item != null) {
            this.items.add(item);
            this.totalCount = this.items.size();
            this.activeCount = (int) this.items.stream()
                    .filter(i -> i.getIsActive() != null && i.getIsActive())
                    .count();
            this.isEmpty = this.items.isEmpty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchlistCollectionResponse that = (WatchlistCollectionResponse) o;
        return Objects.equals(items, that.items) &&
               Objects.equals(totalCount, that.totalCount) &&
               Objects.equals(activeCount, that.activeCount) &&
               Objects.equals(isEmpty, that.isEmpty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, totalCount, activeCount, isEmpty);
    }

    @Override
    public String toString() {
        return "WatchlistCollectionResponse{" +
               "totalCount=" + totalCount +
               ", activeCount=" + activeCount +
               ", isEmpty=" + isEmpty +
               ", items=" + items.size() + " items" +
               '}';
    }
} 