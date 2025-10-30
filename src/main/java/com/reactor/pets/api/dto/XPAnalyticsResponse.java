package com.reactor.pets.api.dto;

import lombok.Value;

/**
 * Phase 7E: XP Analytics response containing progression metrics.
 * Provides insights into XP earning rates and multipliers.
 */
@Value
public class XPAnalyticsResponse {
  long currentXP;
  long lifetimeXPEarned;
  long totalXPSpent;
  double highestXPMultiplier;
  double xpPerMinute; // Calculated based on recent XP earning rate

  public static XPAnalyticsResponse from(
      long currentXP,
      long lifetimeXP,
      long totalSpent,
      double highestMultiplier,
      double xpPerMinute) {
    return new XPAnalyticsResponse(
        currentXP,
        lifetimeXP,
        totalSpent,
        highestMultiplier,
        xpPerMinute);
  }
}
