package io.strategiz.data.user.model.watchlist;

import io.strategiz.data.base.entity.BaseEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Root aggregate for user watchlists.
 * Represents a user's collection of market items they're tracking.
 */
public class Watchlist extends BaseEntity {
    
    private String id;
    private String userId;
    private String name;
    private String description;
    private boolean isDefault;
    private List<String> itemIds = new ArrayList<>();
    
    public Watchlist() {}
    
    public Watchlist(String userId, String name, String createdBy) {
        super(createdBy);
        this.userId = userId;
        this.name = name;
        this.isDefault = false;
    }
    
    public Watchlist(String userId, String name, String description, boolean isDefault, String createdBy) {
        super(createdBy);
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.isDefault = isDefault;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String getCollectionName() {
        return "watchlists";
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public List<String> getItemIds() {
        return itemIds;
    }
    
    public void setItemIds(List<String> itemIds) {
        this.itemIds = itemIds != null ? itemIds : new ArrayList<>();
    }
    
    public void addItem(String itemId) {
        if (itemId != null && !itemIds.contains(itemId)) {
            itemIds.add(itemId);
        }
    }
    
    public boolean removeItem(String itemId) {
        return itemIds.remove(itemId);
    }
    
    public boolean containsItem(String itemId) {
        return itemIds.contains(itemId);
    }
    
    public int getItemCount() {
        return itemIds.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Watchlist watchlist = (Watchlist) o;
        return Objects.equals(id, watchlist.id) &&
               Objects.equals(userId, watchlist.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, userId);
    }
    
    @Override
    public String toString() {
        return "Watchlist{" +
               "id='" + id + '\'' +
               ", userId='" + userId + '\'' +
               ", name='" + name + '\'' +
               ", isDefault=" + isDefault +
               ", itemCount=" + getItemCount() +
               ", audit=" + getAuditFields() +
               '}';
    }
}