package io.strategiz.data.marketdata.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Standard timeframe constants for market data.
 *
 * <p>IMPORTANT: Use these constants throughout all layers for consistency.
 * The canonical format is the primary value (e.g., "1Day"), but legacy
 * formats are supported for backward compatibility.
 */
public final class Timeframe {

  private Timeframe() {
    // Prevent instantiation
  }

  // === CANONICAL VALUES (use these for storage and API contracts) ===

  /** 1 minute bars. */
  public static final String ONE_MINUTE = "1Min";

  /** 5 minute bars. */
  public static final String FIVE_MINUTES = "5Min";

  /** 15 minute bars. */
  public static final String FIFTEEN_MINUTES = "15Min";

  /** 30 minute bars. */
  public static final String THIRTY_MINUTES = "30Min";

  /** 1 hour bars. */
  public static final String ONE_HOUR = "1Hour";

  /** 4 hour bars. */
  public static final String FOUR_HOURS = "4Hour";

  /** Daily bars. */
  public static final String ONE_DAY = "1Day";

  /** Weekly bars. */
  public static final String ONE_WEEK = "1Week";

  /** Monthly bars. */
  public static final String ONE_MONTH = "1Month";

  // === LEGACY ALIASES (for backward compatibility) ===

  /** Legacy alias for 1 minute. */
  public static final String LEGACY_1M = "1m";

  /** Legacy alias for 5 minutes. */
  public static final String LEGACY_5M = "5m";

  /** Legacy alias for 15 minutes. */
  public static final String LEGACY_15M = "15m";

  /** Legacy alias for 30 minutes. */
  public static final String LEGACY_30M = "30m";

  /** Legacy alias for 1 hour. */
  public static final String LEGACY_1H = "1h";

  /** Legacy alias for 4 hours. */
  public static final String LEGACY_4H = "4h";

  /** Legacy alias for daily. */
  public static final String LEGACY_1D = "1D";

  /** Legacy alias for weekly. */
  public static final String LEGACY_1W = "1W";

  /** Legacy alias for monthly. */
  public static final String LEGACY_1MON = "1M";

  /** All valid canonical timeframe values. */
  public static final Set<String> VALID_TIMEFRAMES =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
          ONE_MINUTE, FIVE_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES,
          ONE_HOUR, FOUR_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH
      )));

  /**
   * Normalize a timeframe string to its canonical form.
   * Handles legacy formats like "1D" -&gt; "1Day", "1h" -&gt; "1Hour".
   *
   * @param timeframe Input timeframe (may be legacy format)
   * @return Canonical timeframe format, or the input if not recognized
   */
  public static String normalize(String timeframe) {
    if (timeframe == null || timeframe.isEmpty()) {
      return ONE_DAY; // Default
    }

    // Already canonical
    if (VALID_TIMEFRAMES.contains(timeframe)) {
      return timeframe;
    }

    // Map legacy formats to canonical
    switch (timeframe.toLowerCase()) {
      case "1m":
      case "1min":
        return ONE_MINUTE;
      case "5m":
      case "5min":
        return FIVE_MINUTES;
      case "15m":
      case "15min":
        return FIFTEEN_MINUTES;
      case "30m":
      case "30min":
        return THIRTY_MINUTES;
      case "1h":
      case "1hour":
      case "60m":
        return ONE_HOUR;
      case "4h":
      case "4hour":
        return FOUR_HOURS;
      case "1d":
      case "1day":
      case "d":
      case "day":
      case "daily":
        return ONE_DAY;
      case "1w":
      case "1week":
      case "w":
      case "week":
      case "weekly":
        return ONE_WEEK;
      case "1mon":
      case "1month":
      case "month":
      case "monthly":
        return ONE_MONTH;
      default:
        return timeframe; // Return as-is if not recognized
    }
  }

  /**
   * Get all possible aliases for a canonical timeframe (for queries).
   * Useful when querying data that may have been stored with legacy format.
   *
   * @param canonicalTimeframe The canonical timeframe format
   * @return Set of all possible timeframe strings that represent this timeframe
   */
  public static Set<String> getAliases(String canonicalTimeframe) {
    Set<String> aliases = new HashSet<>();
    aliases.add(canonicalTimeframe);

    switch (canonicalTimeframe) {
      case ONE_MINUTE:
        aliases.add("1m");
        aliases.add("1min");
        break;
      case FIVE_MINUTES:
        aliases.add("5m");
        aliases.add("5min");
        break;
      case FIFTEEN_MINUTES:
        aliases.add("15m");
        aliases.add("15min");
        break;
      case THIRTY_MINUTES:
        aliases.add("30m");
        aliases.add("30min");
        break;
      case ONE_HOUR:
        aliases.add("1h");
        aliases.add("1hour");
        aliases.add("60m");
        break;
      case FOUR_HOURS:
        aliases.add("4h");
        aliases.add("4hour");
        break;
      case ONE_DAY:
        aliases.add("1D");
        aliases.add("1d");
        aliases.add("day");
        aliases.add("daily");
        break;
      case ONE_WEEK:
        aliases.add("1W");
        aliases.add("1w");
        aliases.add("week");
        aliases.add("weekly");
        break;
      case ONE_MONTH:
        aliases.add("1M");
        aliases.add("1mon");
        aliases.add("month");
        aliases.add("monthly");
        break;
      default:
        break;
    }

    return aliases;
  }

  /**
   * Check if a timeframe string is valid (canonical or legacy).
   *
   * @param timeframe The timeframe to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValid(String timeframe) {
    if (timeframe == null || timeframe.isEmpty()) {
      return false;
    }
    String normalized = normalize(timeframe);
    return VALID_TIMEFRAMES.contains(normalized);
  }
}
