package io.strategiz.client.yahoo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Yahoo Finance Balance Sheet DTO.
 *
 * Contains balance sheet data including assets, liabilities, and equity.
 * Corresponds to the balanceSheetHistory section of Yahoo's quote summary.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooBalanceSheet {

	@JsonProperty("totalAssets")
	private BigDecimal totalAssets;

	@JsonProperty("totalLiab")
	private BigDecimal totalLiabilities;

	@JsonProperty("totalStockholderEquity")
	private BigDecimal totalStockholderEquity;

	@JsonProperty("totalCurrentAssets")
	private BigDecimal totalCurrentAssets;

	@JsonProperty("totalCurrentLiabilities")
	private BigDecimal totalCurrentLiabilities;

	@JsonProperty("cash")
	private BigDecimal cash;

	@JsonProperty("cashAndCashEquivalents")
	private BigDecimal cashAndCashEquivalents;

	@JsonProperty("shortTermInvestments")
	private BigDecimal shortTermInvestments;

	@JsonProperty("netReceivables")
	private BigDecimal netReceivables;

	@JsonProperty("inventory")
	private BigDecimal inventory;

	@JsonProperty("otherCurrentAssets")
	private BigDecimal otherCurrentAssets;

	@JsonProperty("propertyPlantEquipment")
	private BigDecimal propertyPlantEquipment;

	@JsonProperty("goodWill")
	private BigDecimal goodWill;

	@JsonProperty("intangibleAssets")
	private BigDecimal intangibleAssets;

	@JsonProperty("longTermInvestments")
	private BigDecimal longTermInvestments;

	@JsonProperty("otherAssets")
	private BigDecimal otherAssets;

	@JsonProperty("accountsPayable")
	private BigDecimal accountsPayable;

	@JsonProperty("shortLongTermDebt")
	private BigDecimal shortLongTermDebt;

	@JsonProperty("otherCurrentLiab")
	private BigDecimal otherCurrentLiabilities;

	@JsonProperty("longTermDebt")
	private BigDecimal longTermDebt;

	@JsonProperty("otherLiab")
	private BigDecimal otherLiabilities;

	@JsonProperty("commonStock")
	private BigDecimal commonStock;

	@JsonProperty("retainedEarnings")
	private BigDecimal retainedEarnings;

	@JsonProperty("treasuryStock")
	private BigDecimal treasuryStock;

	@JsonProperty("capitalSurplus")
	private BigDecimal capitalSurplus;

	// Constructors
	public YahooBalanceSheet() {
	}

	// Getters and Setters
	public BigDecimal getTotalAssets() {
		return totalAssets;
	}

	public void setTotalAssets(BigDecimal totalAssets) {
		this.totalAssets = totalAssets;
	}

	public BigDecimal getTotalLiabilities() {
		return totalLiabilities;
	}

	public void setTotalLiabilities(BigDecimal totalLiabilities) {
		this.totalLiabilities = totalLiabilities;
	}

	public BigDecimal getTotalStockholderEquity() {
		return totalStockholderEquity;
	}

	public void setTotalStockholderEquity(BigDecimal totalStockholderEquity) {
		this.totalStockholderEquity = totalStockholderEquity;
	}

	public BigDecimal getTotalCurrentAssets() {
		return totalCurrentAssets;
	}

	public void setTotalCurrentAssets(BigDecimal totalCurrentAssets) {
		this.totalCurrentAssets = totalCurrentAssets;
	}

	public BigDecimal getTotalCurrentLiabilities() {
		return totalCurrentLiabilities;
	}

	public void setTotalCurrentLiabilities(BigDecimal totalCurrentLiabilities) {
		this.totalCurrentLiabilities = totalCurrentLiabilities;
	}

	public BigDecimal getCash() {
		return cash;
	}

	public void setCash(BigDecimal cash) {
		this.cash = cash;
	}

	public BigDecimal getCashAndCashEquivalents() {
		return cashAndCashEquivalents;
	}

	public void setCashAndCashEquivalents(BigDecimal cashAndCashEquivalents) {
		this.cashAndCashEquivalents = cashAndCashEquivalents;
	}

	public BigDecimal getShortTermInvestments() {
		return shortTermInvestments;
	}

	public void setShortTermInvestments(BigDecimal shortTermInvestments) {
		this.shortTermInvestments = shortTermInvestments;
	}

	public BigDecimal getNetReceivables() {
		return netReceivables;
	}

	public void setNetReceivables(BigDecimal netReceivables) {
		this.netReceivables = netReceivables;
	}

	public BigDecimal getInventory() {
		return inventory;
	}

	public void setInventory(BigDecimal inventory) {
		this.inventory = inventory;
	}

	public BigDecimal getOtherCurrentAssets() {
		return otherCurrentAssets;
	}

	public void setOtherCurrentAssets(BigDecimal otherCurrentAssets) {
		this.otherCurrentAssets = otherCurrentAssets;
	}

	public BigDecimal getPropertyPlantEquipment() {
		return propertyPlantEquipment;
	}

	public void setPropertyPlantEquipment(BigDecimal propertyPlantEquipment) {
		this.propertyPlantEquipment = propertyPlantEquipment;
	}

	public BigDecimal getGoodWill() {
		return goodWill;
	}

	public void setGoodWill(BigDecimal goodWill) {
		this.goodWill = goodWill;
	}

	public BigDecimal getIntangibleAssets() {
		return intangibleAssets;
	}

	public void setIntangibleAssets(BigDecimal intangibleAssets) {
		this.intangibleAssets = intangibleAssets;
	}

	public BigDecimal getLongTermInvestments() {
		return longTermInvestments;
	}

	public void setLongTermInvestments(BigDecimal longTermInvestments) {
		this.longTermInvestments = longTermInvestments;
	}

	public BigDecimal getOtherAssets() {
		return otherAssets;
	}

	public void setOtherAssets(BigDecimal otherAssets) {
		this.otherAssets = otherAssets;
	}

	public BigDecimal getAccountsPayable() {
		return accountsPayable;
	}

	public void setAccountsPayable(BigDecimal accountsPayable) {
		this.accountsPayable = accountsPayable;
	}

	public BigDecimal getShortLongTermDebt() {
		return shortLongTermDebt;
	}

	public void setShortLongTermDebt(BigDecimal shortLongTermDebt) {
		this.shortLongTermDebt = shortLongTermDebt;
	}

	public BigDecimal getOtherCurrentLiabilities() {
		return otherCurrentLiabilities;
	}

	public void setOtherCurrentLiabilities(BigDecimal otherCurrentLiabilities) {
		this.otherCurrentLiabilities = otherCurrentLiabilities;
	}

	public BigDecimal getLongTermDebt() {
		return longTermDebt;
	}

	public void setLongTermDebt(BigDecimal longTermDebt) {
		this.longTermDebt = longTermDebt;
	}

	public BigDecimal getOtherLiabilities() {
		return otherLiabilities;
	}

	public void setOtherLiabilities(BigDecimal otherLiabilities) {
		this.otherLiabilities = otherLiabilities;
	}

	public BigDecimal getCommonStock() {
		return commonStock;
	}

	public void setCommonStock(BigDecimal commonStock) {
		this.commonStock = commonStock;
	}

	public BigDecimal getRetainedEarnings() {
		return retainedEarnings;
	}

	public void setRetainedEarnings(BigDecimal retainedEarnings) {
		this.retainedEarnings = retainedEarnings;
	}

	public BigDecimal getTreasuryStock() {
		return treasuryStock;
	}

	public void setTreasuryStock(BigDecimal treasuryStock) {
		this.treasuryStock = treasuryStock;
	}

	public BigDecimal getCapitalSurplus() {
		return capitalSurplus;
	}

	public void setCapitalSurplus(BigDecimal capitalSurplus) {
		this.capitalSurplus = capitalSurplus;
	}

}
