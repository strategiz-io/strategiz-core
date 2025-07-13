package io.strategiz.data.user.model.watchlist;

import java.util.Date;
import java.util.Objects;

/**
 * Response model for reading watchlist items.
 * Contains all fields for display purposes.
 */
public class ReadWatchlistItemResponse {
    
    private String id;
    private String symbol;
    private String name;
    private String type;
    private Date addedAt;
    private Date createdAt;
    private Date modifiedAt;
    private Integer version;
    private Boolean isActive;

    // Constructors
    public ReadWatchlistItemResponse() {}

    /**
     * Creates a response from a MarketWatchlistItem entity.
     * 
     * @param entity The entity to convert
     * @return ReadWatchlistItemResponse
     */
    public static ReadWatchlistItemResponse fromEntity(MarketWatchlistItem entity) {
        ReadWatchlistItemResponse response = new ReadWatchlistItemResponse();
        response.setId(entity.getId());
        response.setSymbol(entity.getSymbol());
        response.setName(entity.getName());
        response.setType(entity.getType());
        response.setAddedAt(entity.getAddedAt());
        response.setCreatedAt(entity.getCreatedAt());
        response.setModifiedAt(entity.getModifiedAt());
        response.setVersion(entity.getVersion() != null ? entity.getVersion().intValue() : null);
        response.setIsActive(entity.isActive());
        return response;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Date addedAt) {
        this.addedAt = addedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReadWatchlistItemResponse that = (ReadWatchlistItemResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(name, that.name) &&
               Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, name, type);
    }

    @Override
    public String toString() {
        return "ReadWatchlistItemResponse{" +
               "id='" + id + '\'' +
               ", symbol='" + symbol + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", addedAt=" + addedAt +
               ", isActive=" + isActive +
               '}';
    }
} 