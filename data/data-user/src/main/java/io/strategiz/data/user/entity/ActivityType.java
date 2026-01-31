package io.strategiz.data.user.entity;

/**
 * Types of activities that can be recorded in the activity feed.
 */
public enum ActivityType {

	// Strategy-related activities
	STRATEGY_PUBLISHED("Published a new strategy"), STRATEGY_UPDATED("Updated a strategy"),
	STRATEGY_ARCHIVED("Archived a strategy"),

	// Trading activities
	TRADE_EXECUTED("Executed a trade"), ALERT_TRIGGERED("Alert was triggered"), BOT_STARTED("Started a trading bot"),
	BOT_STOPPED("Stopped a trading bot"),

	// Social activities
	FOLLOWED_USER("Started following a user"), SUBSCRIBED_STRATEGY("Subscribed to a strategy"),

	// Account activities
	PROVIDER_CONNECTED("Connected a trading provider"), ACHIEVEMENT_UNLOCKED("Unlocked an achievement"),
	MILESTONE_REACHED("Reached a portfolio milestone");

	private final String displayText;

	ActivityType(String displayText) {
		this.displayText = displayText;
	}

	public String getDisplayText() {
		return displayText;
	}

	@Override
	public String toString() {
		return name();
	}

}
