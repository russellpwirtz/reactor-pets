package com.reactor.pets.api.dto;

import com.reactor.pets.aggregate.PetStage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Global statistics across all pets")
public class StatisticsResponse {

  @Schema(description = "Total number of pets created", example = "42")
  private int totalPetsCreated;

  @Schema(description = "Total number of pets that have died", example = "5")
  private int totalPetsDied;

  @Schema(description = "Number of pets currently alive", example = "37")
  private int currentlyAlive;

  @Schema(description = "Average lifespan of deceased pets in ticks", example = "125.5")
  private double averageLifespan;

  @Schema(description = "Name of the longest-lived pet", example = "Max")
  private String longestLivedPetName;

  @Schema(
      description = "ID of the longest-lived pet",
      example = "123e4567-e89b-12d3-a456-426614174000")
  private String longestLivedPetId;

  @Schema(description = "Age of the longest-lived pet in ticks", example = "250")
  private int longestLivedPetAge;

  @Schema(description = "Distribution of pets by evolution stage")
  private Map<PetStage, Integer> stageDistribution;

  @Schema(description = "Last updated timestamp")
  private Instant lastUpdated;
}
