package io.strategiz.data.strategy.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Strategy comment entity - represents a comment on a published strategy.
 *
 * Collection: strategy_comments (top-level)
 *
 * Comments can only be added to published (public) strategies.
 * Supports threaded replies via parentCommentId.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("strategy_comments")
public class StrategyCommentEntity extends BaseEntity {

    @DocumentId
    @PropertyName("id")
    @JsonProperty("id")
    private String id;

    @PropertyName("strategyId")
    @JsonProperty("strategyId")
    @NotBlank(message = "Strategy ID is required")
    private String strategyId;

    @PropertyName("userId")
    @JsonProperty("userId")
    @NotBlank(message = "User ID is required")
    private String userId;

    @PropertyName("content")
    @JsonProperty("content")
    @NotBlank(message = "Comment content is required")
    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String content;

    @PropertyName("parentCommentId")
    @JsonProperty("parentCommentId")
    private String parentCommentId;

    @PropertyName("likeCount")
    @JsonProperty("likeCount")
    private Integer likeCount = 0;

    @PropertyName("replyCount")
    @JsonProperty("replyCount")
    private Integer replyCount = 0;

    @PropertyName("commentedAt")
    @JsonProperty("commentedAt")
    private Timestamp commentedAt;

    @PropertyName("editedAt")
    @JsonProperty("editedAt")
    private Timestamp editedAt;

    // Denormalized user info for display
    @PropertyName("userName")
    @JsonProperty("userName")
    private String userName;

    @PropertyName("userPhotoURL")
    @JsonProperty("userPhotoURL")
    private String userPhotoURL;

    // Constructors
    public StrategyCommentEntity() {
        super();
    }

    public StrategyCommentEntity(String strategyId, String userId, String content) {
        super();
        this.strategyId = strategyId;
        this.userId = userId;
        this.content = content;
        this.commentedAt = Timestamp.now();
    }

    public StrategyCommentEntity(String strategyId, String userId, String content, String parentCommentId) {
        this(strategyId, userId, content);
        this.parentCommentId = parentCommentId;
    }

    // Getters and Setters
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public Timestamp getCommentedAt() {
        return commentedAt;
    }

    public void setCommentedAt(Timestamp commentedAt) {
        this.commentedAt = commentedAt;
    }

    public Timestamp getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Timestamp editedAt) {
        this.editedAt = editedAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhotoURL() {
        return userPhotoURL;
    }

    public void setUserPhotoURL(String userPhotoURL) {
        this.userPhotoURL = userPhotoURL;
    }

    // Helper methods

    /**
     * Check if this is a reply to another comment.
     */
    public boolean isReply() {
        return parentCommentId != null && !parentCommentId.isEmpty();
    }

    /**
     * Check if this is a top-level comment.
     */
    public boolean isTopLevel() {
        return !isReply();
    }

    /**
     * Mark the comment as edited.
     */
    public void markAsEdited() {
        this.editedAt = Timestamp.now();
    }

    /**
     * Check if the comment has been edited.
     */
    public boolean isEdited() {
        return editedAt != null;
    }

    /**
     * Increment like count.
     */
    public void incrementLikes() {
        this.likeCount = (this.likeCount != null ? this.likeCount : 0) + 1;
    }

    /**
     * Decrement like count.
     */
    public void decrementLikes() {
        if (this.likeCount != null && this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * Increment reply count.
     */
    public void incrementReplies() {
        this.replyCount = (this.replyCount != null ? this.replyCount : 0) + 1;
    }

    /**
     * Decrement reply count.
     */
    public void decrementReplies() {
        if (this.replyCount != null && this.replyCount > 0) {
            this.replyCount--;
        }
    }

    @Override
    public String toString() {
        return "StrategyCommentEntity{" +
                "id='" + id + '\'' +
                ", strategyId='" + strategyId + '\'' +
                ", userId='" + userId + '\'' +
                ", content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", parentCommentId='" + parentCommentId + '\'' +
                ", likeCount=" + likeCount +
                ", commentedAt=" + commentedAt +
                ", isActive=" + getIsActive() +
                '}';
    }
}
