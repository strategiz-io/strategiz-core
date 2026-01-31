package io.strategiz.service.dashboard.model.response;

import io.strategiz.service.dashboard.model.watchlist.WatchlistItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Response object for watchlist collection data
 */
public class WatchlistCollectionResponse {

	private String userId;

	private List<WatchlistItem> items;

	private Integer totalCount;

	private Integer activeCount;

	private Boolean isEmpty;

	private Boolean demoMode;

	private LocalDateTime lastUpdated;

	// Constructors
	public WatchlistCollectionResponse() {
		this.isEmpty = true;
		this.totalCount = 0;
		this.activeCount = 0;
		this.lastUpdated = LocalDateTime.now();
	}

	public WatchlistCollectionResponse(String userId, List<WatchlistItem> items) {
		this.userId = userId;
		this.items = items;
		this.totalCount = items != null ? items.size() : 0;
		this.activeCount = this.totalCount; // Assume all items are active for now
		this.isEmpty = items == null || items.isEmpty();
		this.lastUpdated = LocalDateTime.now();
	}

	// Getters and Setters
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public List<WatchlistItem> getItems() {
		return items;
	}

	public void setItems(List<WatchlistItem> items) {
		this.items = items;
		this.totalCount = items != null ? items.size() : 0;
		this.activeCount = this.totalCount; // Assume all items are active for now
		this.isEmpty = items == null || items.isEmpty();
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

	public Boolean getDemoMode() {
		return demoMode;
	}

	public void setDemoMode(Boolean demoMode) {
		this.demoMode = demoMode;
	}

	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(LocalDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	// Convenience methods
	public boolean hasItems() {
		return items != null && !items.isEmpty();
	}

	public int getItemCount() {
		return items != null ? items.size() : 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		WatchlistCollectionResponse that = (WatchlistCollectionResponse) o;
		return Objects.equals(userId, that.userId) && Objects.equals(items, that.items)
				&& Objects.equals(totalCount, that.totalCount) && Objects.equals(activeCount, that.activeCount)
				&& Objects.equals(isEmpty, that.isEmpty) && Objects.equals(demoMode, that.demoMode)
				&& Objects.equals(lastUpdated, that.lastUpdated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, items, totalCount, activeCount, isEmpty, demoMode, lastUpdated);
	}

	@Override
	public String toString() {
		return "WatchlistCollectionResponse{" + "userId='" + userId + '\'' + ", itemCount=" + getItemCount()
				+ ", totalCount=" + totalCount + ", activeCount=" + activeCount + ", isEmpty=" + isEmpty + ", demoMode="
				+ demoMode + ", lastUpdated=" + lastUpdated + '}';
	}

}