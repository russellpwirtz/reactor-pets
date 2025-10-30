package com.reactor.pets.api.dto;

import com.reactor.pets.domain.UpgradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Player progression information")
public class PlayerProgressionResponse {
  @Schema(description = "Player ID", example = "PLAYER_1")
  private String playerId;

  @Schema(description = "Current spendable XP", example = "1250")
  private long currentXP;

  @Schema(description = "Lifetime XP earned (never decreases)", example = "5000")
  private long lifetimeXP;

  @Schema(description = "Current XP multiplier (highest among alive pets)", example = "1.5")
  private double xpMultiplier;

  @Schema(description = "Highest XP multiplier ever achieved", example = "2.0")
  private double highestMultiplier;

  @Schema(description = "Total XP spent on purchases", example = "3750")
  private long totalXPSpent;

  @Schema(description = "Set of permanent upgrades purchased by the player")
  private Set<UpgradeType> permanentUpgrades;
}
