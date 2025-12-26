package io.strategiz.client.yahoo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Yahoo Finance Income Statement DTO.
 *
 * Contains income statement data including revenue, expenses, and earnings.
 * Corresponds to the incomeStatementHistory section of Yahoo's quote summary.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooIncomeStatement {

	@JsonProperty("totalRevenue")
	private BigDecimal totalRevenue;

	@JsonProperty("costOfRevenue")
	private BigDecimal costOfRevenue;

	@JsonProperty("grossProfit")
	private BigDecimal grossProfit;

	@JsonProperty("researchDevelopment")
	private BigDecimal researchDevelopment;

	@JsonProperty("sellingGeneralAdministrative")
	private BigDecimal sellingGeneralAdministrative;

	@JsonProperty("totalOperatingExpenses")
	private BigDecimal totalOperatingExpenses;

	@JsonProperty("operatingIncome")
	private BigDecimal operatingIncome;

	@JsonProperty("totalOtherIncomeExpenseNet")
	private BigDecimal totalOtherIncomeExpenseNet;

	@JsonProperty("ebit")
	private BigDecimal ebit;

	@JsonProperty("interestExpense")
	private BigDecimal interestExpense;

	@JsonProperty("incomeBeforeTax")
	private BigDecimal incomeBeforeTax;

	@JsonProperty("incomeTaxExpense")
	private BigDecimal incomeTaxExpense;

	@JsonProperty("netIncome")
	private BigDecimal netIncome;

	@JsonProperty("netIncomeApplicableToCommonShares")
	private BigDecimal netIncomeApplicableToCommonShares;

	@JsonProperty("discontinuedOperations")
	private BigDecimal discontinuedOperations;

	@JsonProperty("extraordinaryItems")
	private BigDecimal extraordinaryItems;

	@JsonProperty("effectOfAccountingCharges")
	private BigDecimal effectOfAccountingCharges;

	@JsonProperty("otherItems")
	private BigDecimal otherItems;

	@JsonProperty("minorityInterest")
	private BigDecimal minorityInterest;

	// Constructors
	public YahooIncomeStatement() {
	}

	// Getters and Setters
	public BigDecimal getTotalRevenue() {
		return totalRevenue;
	}

	public void setTotalRevenue(BigDecimal totalRevenue) {
		this.totalRevenue = totalRevenue;
	}

	public BigDecimal getCostOfRevenue() {
		return costOfRevenue;
	}

	public void setCostOfRevenue(BigDecimal costOfRevenue) {
		this.costOfRevenue = costOfRevenue;
	}

	public BigDecimal getGrossProfit() {
		return grossProfit;
	}

	public void setGrossProfit(BigDecimal grossProfit) {
		this.grossProfit = grossProfit;
	}

	public BigDecimal getResearchDevelopment() {
		return researchDevelopment;
	}

	public void setResearchDevelopment(BigDecimal researchDevelopment) {
		this.researchDevelopment = researchDevelopment;
	}

	public BigDecimal getSellingGeneralAdministrative() {
		return sellingGeneralAdministrative;
	}

	public void setSellingGeneralAdministrative(BigDecimal sellingGeneralAdministrative) {
		this.sellingGeneralAdministrative = sellingGeneralAdministrative;
	}

	public BigDecimal getTotalOperatingExpenses() {
		return totalOperatingExpenses;
	}

	public void setTotalOperatingExpenses(BigDecimal totalOperatingExpenses) {
		this.totalOperatingExpenses = totalOperatingExpenses;
	}

	public BigDecimal getOperatingIncome() {
		return operatingIncome;
	}

	public void setOperatingIncome(BigDecimal operatingIncome) {
		this.operatingIncome = operatingIncome;
	}

	public BigDecimal getTotalOtherIncomeExpenseNet() {
		return totalOtherIncomeExpenseNet;
	}

	public void setTotalOtherIncomeExpenseNet(BigDecimal totalOtherIncomeExpenseNet) {
		this.totalOtherIncomeExpenseNet = totalOtherIncomeExpenseNet;
	}

	public BigDecimal getEbit() {
		return ebit;
	}

	public void setEbit(BigDecimal ebit) {
		this.ebit = ebit;
	}

	public BigDecimal getInterestExpense() {
		return interestExpense;
	}

	public void setInterestExpense(BigDecimal interestExpense) {
		this.interestExpense = interestExpense;
	}

	public BigDecimal getIncomeBeforeTax() {
		return incomeBeforeTax;
	}

	public void setIncomeBeforeTax(BigDecimal incomeBeforeTax) {
		this.incomeBeforeTax = incomeBeforeTax;
	}

	public BigDecimal getIncomeTaxExpense() {
		return incomeTaxExpense;
	}

	public void setIncomeTaxExpense(BigDecimal incomeTaxExpense) {
		this.incomeTaxExpense = incomeTaxExpense;
	}

	public BigDecimal getNetIncome() {
		return netIncome;
	}

	public void setNetIncome(BigDecimal netIncome) {
		this.netIncome = netIncome;
	}

	public BigDecimal getNetIncomeApplicableToCommonShares() {
		return netIncomeApplicableToCommonShares;
	}

	public void setNetIncomeApplicableToCommonShares(BigDecimal netIncomeApplicableToCommonShares) {
		this.netIncomeApplicableToCommonShares = netIncomeApplicableToCommonShares;
	}

	public BigDecimal getDiscontinuedOperations() {
		return discontinuedOperations;
	}

	public void setDiscontinuedOperations(BigDecimal discontinuedOperations) {
		this.discontinuedOperations = discontinuedOperations;
	}

	public BigDecimal getExtraordinaryItems() {
		return extraordinaryItems;
	}

	public void setExtraordinaryItems(BigDecimal extraordinaryItems) {
		this.extraordinaryItems = extraordinaryItems;
	}

	public BigDecimal getEffectOfAccountingCharges() {
		return effectOfAccountingCharges;
	}

	public void setEffectOfAccountingCharges(BigDecimal effectOfAccountingCharges) {
		this.effectOfAccountingCharges = effectOfAccountingCharges;
	}

	public BigDecimal getOtherItems() {
		return otherItems;
	}

	public void setOtherItems(BigDecimal otherItems) {
		this.otherItems = otherItems;
	}

	public BigDecimal getMinorityInterest() {
		return minorityInterest;
	}

	public void setMinorityInterest(BigDecimal minorityInterest) {
		this.minorityInterest = minorityInterest;
	}

}
