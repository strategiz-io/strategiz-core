package io.strategiz.service.console.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response model for job execution history with statistics.
 * Wraps a list of executions along with aggregate stats and pagination info.
 */
public class JobExecutionHistoryResponse {

    @JsonProperty("executions")
    private List<JobExecutionRecord> executions;

    @JsonProperty("stats")
    private Map<String, Object> stats; // successCount, failureCount, successRate, avgDurationMs, periodDays

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("pageSize")
    private Integer pageSize;

    @JsonProperty("totalCount")
    private Long totalCount;

    @JsonProperty("totalPages")
    private Integer totalPages;

    // Constructors
    public JobExecutionHistoryResponse() {
    }

    public JobExecutionHistoryResponse(
            List<JobExecutionRecord> executions,
            Map<String, Object> stats,
            Integer page,
            Integer pageSize,
            Long totalCount,
            Integer totalPages) {
        this.executions = executions;
        this.stats = stats;
        this.page = page;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
    }

    // Getters and Setters
    public List<JobExecutionRecord> getExecutions() {
        return executions;
    }

    public void setExecutions(List<JobExecutionRecord> executions) {
        this.executions = executions;
    }

    public Map<String, Object> getStats() {
        return stats;
    }

    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
}
