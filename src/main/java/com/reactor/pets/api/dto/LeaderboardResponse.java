package com.reactor.pets.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Leaderboard of top pets by a specific metric")
public class LeaderboardResponse {

  @Schema(description = "Type of leaderboard (AGE, HAPPINESS, HEALTH)", example = "AGE")
  private String type;

  @Schema(description = "List of top pets")
  private List<PetStatusResponse> pets;

  @Schema(description = "Total number of pets in the leaderboard", example = "10")
  private int totalCount;
}
