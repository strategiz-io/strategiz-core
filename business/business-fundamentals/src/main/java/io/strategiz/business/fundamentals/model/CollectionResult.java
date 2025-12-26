package io.strategiz.business.fundamentals.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a fundamentals collection operation.
 *
 * Tracks success/failure counts and individual symbol results.
 */
public class CollectionResult {

	private int totalSymbols;

	private int successCount;

	private int errorCount;

	private Instant startTime;

	private Instant endTime;

	private List<SymbolResult> symbolResults = new ArrayList<>();

	public CollectionResult() {
		this.startTime = Instant.now();
	}

	public void complete() {
		this.endTime = Instant.now();
	}

	public long getDurationSeconds() {
		if (endTime == null) {
			return 0;
		}
		return endTime.getEpochSecond() - startTime.getEpochSecond();
	}

	public void addSymbolResult(SymbolResult result) {
		this.symbolResults.add(result);
		if (result.isSuccess()) {
			this.successCount++;
		}
		else {
			this.errorCount++;
		}
	}

	// Getters and Setters

	public int getTotalSymbols() {
		return totalSymbols;
	}

	public void setTotalSymbols(int totalSymbols) {
		this.totalSymbols = totalSymbols;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public int getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(int errorCount) {
		this.errorCount = errorCount;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public List<SymbolResult> getSymbolResults() {
		return symbolResults;
	}

	public void setSymbolResults(List<SymbolResult> symbolResults) {
		this.symbolResults = symbolResults;
	}

	@Override
	public String toString() {
		return String.format("CollectionResult[total=%d, success=%d, errors=%d, duration=%ds]", totalSymbols,
				successCount, errorCount, getDurationSeconds());
	}

}
