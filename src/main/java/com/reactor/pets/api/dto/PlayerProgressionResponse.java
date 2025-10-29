package com.reactor.pets.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Player progression information")
public class PlayerProgressionResponse {
  @Schema(description = "Player ID", example = "PLAYER")
  private String playerId;

  @Schema(description = "Current spendable XP", example = "1250")
  private long totalXP;

  @Schema(description = "Lifetime XP earned (never decreases)", example = "5000")
  private long lifetimeXPEarned;

  @Schema(description = "Total pets created", example = "3")
  private int totalPetsCreated;

  @Schema(description = "Prestige level", example = "0")
  private int prestigeLevel;
}
