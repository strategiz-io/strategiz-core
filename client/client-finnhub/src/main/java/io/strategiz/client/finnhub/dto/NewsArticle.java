package io.strategiz.client.finnhub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTO representing a news article from Finnhub API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsArticle {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("category")
    private String category;

    @JsonProperty("datetime")
    private Long datetime;

    @JsonProperty("headline")
    private String headline;

    @JsonProperty("image")
    private String image;

    @JsonProperty("related")
    private String related;

    @JsonProperty("source")
    private String source;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("url")
    private String url;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getDatetime() {
        return datetime;
    }

    public void setDatetime(Long datetime) {
        this.datetime = datetime;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getRelated() {
        return related;
    }

    public void setRelated(String related) {
        this.related = related;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get datetime as Instant
     */
    public Instant getDatetimeAsInstant() {
        return datetime != null ? Instant.ofEpochSecond(datetime) : null;
    }

    /**
     * Format for AI context injection
     */
    public String toContextString() {
        return String.format("[%s] %s - %s\nSource: %s | %s",
                getDatetimeAsInstant() != null ? getDatetimeAsInstant().toString() : "Unknown",
                headline,
                summary != null ? summary.substring(0, Math.min(summary.length(), 200)) + "..." : "",
                source,
                related != null ? "Related: " + related : "");
    }

    @Override
    public String toString() {
        return "NewsArticle{" +
                "id=" + id +
                ", headline='" + headline + '\'' +
                ", source='" + source + '\'' +
                ", datetime=" + datetime +
                '}';
    }
}
