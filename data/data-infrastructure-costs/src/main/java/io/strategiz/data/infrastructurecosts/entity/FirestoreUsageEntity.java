package io.strategiz.data.infrastructurecosts.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Firestore usage tracking stored at infrastructure/usage/firestore/{date}
 * Documents read/write counts per collection.
 */
@Collection("firestore")
public class FirestoreUsageEntity extends BaseEntity {

    @DocumentId
    @PropertyName("date")
    @JsonProperty("date")
    private String date; // Format: yyyy-MM-dd

    @PropertyName("totalReads")
    @JsonProperty("totalReads")
    private Long totalReads;

    @PropertyName("totalWrites")
    @JsonProperty("totalWrites")
    private Long totalWrites;

    @PropertyName("totalDeletes")
    @JsonProperty("totalDeletes")
    private Long totalDeletes;

    @PropertyName("readsByCollection")
    @JsonProperty("readsByCollection")
    private Map<String, Long> readsByCollection;

    @PropertyName("writesByCollection")
    @JsonProperty("writesByCollection")
    private Map<String, Long> writesByCollection;

    @PropertyName("deletesByCollection")
    @JsonProperty("deletesByCollection")
    private Map<String, Long> deletesByCollection;

    @PropertyName("estimatedCost")
    @JsonProperty("estimatedCost")
    private BigDecimal estimatedCost;

    @PropertyName("createdAt")
    @JsonProperty("createdAt")
    private Instant createdAt;

    @PropertyName("updatedAt")
    @JsonProperty("updatedAt")
    private Instant updatedAt;

    // Constructors
    public FirestoreUsageEntity() {
        super();
    }

    public FirestoreUsageEntity(String date) {
        super();
        this.date = date;
        this.totalReads = 0L;
        this.totalWrites = 0L;
        this.totalDeletes = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getTotalReads() {
        return totalReads;
    }

    public void setTotalReads(Long totalReads) {
        this.totalReads = totalReads;
    }

    public Long getTotalWrites() {
        return totalWrites;
    }

    public void setTotalWrites(Long totalWrites) {
        this.totalWrites = totalWrites;
    }

    public Long getTotalDeletes() {
        return totalDeletes;
    }

    public void setTotalDeletes(Long totalDeletes) {
        this.totalDeletes = totalDeletes;
    }

    public Map<String, Long> getReadsByCollection() {
        return readsByCollection;
    }

    public void setReadsByCollection(Map<String, Long> readsByCollection) {
        this.readsByCollection = readsByCollection;
    }

    public Map<String, Long> getWritesByCollection() {
        return writesByCollection;
    }

    public void setWritesByCollection(Map<String, Long> writesByCollection) {
        this.writesByCollection = writesByCollection;
    }

    public Map<String, Long> getDeletesByCollection() {
        return deletesByCollection;
    }

    public void setDeletesByCollection(Map<String, Long> deletesByCollection) {
        this.deletesByCollection = deletesByCollection;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String getId() {
        return date;
    }

    @Override
    public void setId(String id) {
        this.date = id;
    }

    /**
     * Increment read count for a collection
     */
    public void incrementReads(String collection, long count) {
        this.totalReads = (this.totalReads == null ? 0L : this.totalReads) + count;
        if (this.readsByCollection != null) {
            this.readsByCollection.merge(collection, count, Long::sum);
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Increment write count for a collection
     */
    public void incrementWrites(String collection, long count) {
        this.totalWrites = (this.totalWrites == null ? 0L : this.totalWrites) + count;
        if (this.writesByCollection != null) {
            this.writesByCollection.merge(collection, count, Long::sum);
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Increment delete count for a collection
     */
    public void incrementDeletes(String collection, long count) {
        this.totalDeletes = (this.totalDeletes == null ? 0L : this.totalDeletes) + count;
        if (this.deletesByCollection != null) {
            this.deletesByCollection.merge(collection, count, Long::sum);
        }
        this.updatedAt = Instant.now();
    }
}
