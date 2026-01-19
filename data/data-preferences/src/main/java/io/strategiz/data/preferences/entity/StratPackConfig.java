package io.strategiz.data.preferences.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.util.ArrayList;
import java.util.List;

/**
 * Configurable STRAT pack settings stored at config/strat-packs/{packId}.
 *
 * <p>This entity allows admin configuration of STRAT token packs
 * without code deploys. Changes take effect immediately.</p>
 *
 * <p>Firestore path: config/strat-packs/{packId}</p>
 *
 * <p>STRAT Pack Model:</p>
 * <ul>
 *   <li>Available to ALL tiers, including Explorer (FREE)</li>
 *   <li>One-time purchases (not subscriptions)</li>
 *   <li>Purchased STRAT adds to wallet balance (persistent, doesn't expire)</li>
 *   <li>Same rate as tier STRAT (no premium for packs)</li>
 * </ul>
 *
 * <p>Example structure:</p>
 * <pre>
 * {
 *   packId: "starter",
 *   displayName: "Starter Pack",
 *   priceCents: 500,     // $5 USD
 *   stratAmount: 500,    // 500 STRAT tokens
 *   enabled: true,
 *   sortOrder: 1
 * }
 * </pre>
 *
 * @see io.strategiz.data.cryptotoken.entity.CryptoWallet
 */
@Collection("stratPackConfig")
public class StratPackConfig extends BaseEntity {

	// Pack IDs (standard packs)
	public static final String PACK_STARTER = "starter";
	public static final String PACK_BASIC = "basic";
	public static final String PACK_STANDARD = "standard";
	public static final String PACK_PRO = "pro";
	public static final String PACK_POWER = "power";

	@DocumentId
	@PropertyName("packId")
	@JsonProperty("packId")
	private String packId;

	@PropertyName("displayName")
	@JsonProperty("displayName")
	private String displayName;

	@PropertyName("description")
	@JsonProperty("description")
	private String description;

	/**
	 * Price in USD cents.
	 * Example: 500 = $5.00
	 */
	@PropertyName("priceCents")
	@JsonProperty("priceCents")
	private Integer priceCents;

	/**
	 * Amount of STRAT tokens received.
	 */
	@PropertyName("stratAmount")
	@JsonProperty("stratAmount")
	private Long stratAmount;

	/**
	 * Whether this pack is currently enabled/purchasable.
	 */
	@PropertyName("enabled")
	@JsonProperty("enabled")
	private Boolean enabled;

	/**
	 * Display order (lower = first).
	 */
	@PropertyName("sortOrder")
	@JsonProperty("sortOrder")
	private Integer sortOrder;

	/**
	 * Stripe Price ID for this pack.
	 */
	@PropertyName("stripePriceId")
	@JsonProperty("stripePriceId")
	private String stripePriceId;

	/**
	 * Badge or label for special packs (e.g., "POPULAR", "BEST VALUE").
	 */
	@PropertyName("badge")
	@JsonProperty("badge")
	private String badge;

	/**
	 * Optional bonus STRAT for promotional packs.
	 */
	@PropertyName("bonusStrat")
	@JsonProperty("bonusStrat")
	private Long bonusStrat;

	public StratPackConfig() {
		super();
		this.enabled = true;
		this.bonusStrat = 0L;
	}

	/**
	 * Get total STRAT received (base + bonus).
	 */
	public Long getTotalStrat() {
		long bonus = bonusStrat != null ? bonusStrat : 0L;
		return (stratAmount != null ? stratAmount : 0L) + bonus;
	}

	/**
	 * Get price per STRAT in cents.
	 */
	public double getPricePerStrat() {
		if (stratAmount == null || stratAmount == 0) {
			return 0;
		}
		return (double) priceCents / getTotalStrat();
	}

	/**
	 * Create all default STRAT packs.
	 */
	public static List<StratPackConfig> createDefaults() {
		List<StratPackConfig> packs = new ArrayList<>();
		packs.add(createStarterPack());
		packs.add(createBasicPack());
		packs.add(createStandardPack());
		packs.add(createProPack());
		packs.add(createPowerPack());
		return packs;
	}

	/**
	 * Create Starter pack ($5 = 500 STRAT).
	 */
	public static StratPackConfig createStarterPack() {
		StratPackConfig pack = new StratPackConfig();
		pack.setPackId(PACK_STARTER);
		pack.setDisplayName("Starter Pack");
		pack.setDescription("Perfect for trying out premium features");
		pack.setPriceCents(500);
		pack.setStratAmount(500L);
		pack.setSortOrder(1);
		return pack;
	}

	/**
	 * Create Basic pack ($10 = 1,000 STRAT).
	 */
	public static StratPackConfig createBasicPack() {
		StratPackConfig pack = new StratPackConfig();
		pack.setPackId(PACK_BASIC);
		pack.setDisplayName("Basic Pack");
		pack.setDescription("Great value for regular users");
		pack.setPriceCents(1000);
		pack.setStratAmount(1000L);
		pack.setSortOrder(2);
		return pack;
	}

	/**
	 * Create Standard pack ($25 = 2,500 STRAT).
	 */
	public static StratPackConfig createStandardPack() {
		StratPackConfig pack = new StratPackConfig();
		pack.setPackId(PACK_STANDARD);
		pack.setDisplayName("Standard Pack");
		pack.setDescription("Most popular choice");
		pack.setPriceCents(2500);
		pack.setStratAmount(2500L);
		pack.setBadge("POPULAR");
		pack.setSortOrder(3);
		return pack;
	}

	/**
	 * Create Pro pack ($50 = 5,000 STRAT).
	 */
	public static StratPackConfig createProPack() {
		StratPackConfig pack = new StratPackConfig();
		pack.setPackId(PACK_PRO);
		pack.setDisplayName("Pro Pack");
		pack.setDescription("For active traders");
		pack.setPriceCents(5000);
		pack.setStratAmount(5000L);
		pack.setSortOrder(4);
		return pack;
	}

	/**
	 * Create Power pack ($100 = 10,000 STRAT).
	 */
	public static StratPackConfig createPowerPack() {
		StratPackConfig pack = new StratPackConfig();
		pack.setPackId(PACK_POWER);
		pack.setDisplayName("Power Pack");
		pack.setDescription("Best value for power users");
		pack.setPriceCents(10000);
		pack.setStratAmount(10000L);
		pack.setBadge("BEST VALUE");
		pack.setSortOrder(5);
		return pack;
	}

	// Getters and Setters

	@Override
	public String getId() {
		return packId;
	}

	@Override
	public void setId(String id) {
		this.packId = id;
	}

	public String getPackId() {
		return packId;
	}

	public void setPackId(String packId) {
		this.packId = packId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getPriceCents() {
		return priceCents;
	}

	public void setPriceCents(Integer priceCents) {
		this.priceCents = priceCents;
	}

	public Long getStratAmount() {
		return stratAmount;
	}

	public void setStratAmount(Long stratAmount) {
		this.stratAmount = stratAmount;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getStripePriceId() {
		return stripePriceId;
	}

	public void setStripePriceId(String stripePriceId) {
		this.stripePriceId = stripePriceId;
	}

	public String getBadge() {
		return badge;
	}

	public void setBadge(String badge) {
		this.badge = badge;
	}

	public Long getBonusStrat() {
		return bonusStrat;
	}

	public void setBonusStrat(Long bonusStrat) {
		this.bonusStrat = bonusStrat;
	}

}
