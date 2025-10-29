package com.reactor.pets.api.controller;

import com.reactor.pets.api.dto.LeaderboardResponse;
import com.reactor.pets.api.dto.PetStatusResponse;
import com.reactor.pets.api.dto.StatisticsResponse;
import com.reactor.pets.query.GetLeaderboardQuery;
import com.reactor.pets.query.PetStatistics;
import com.reactor.pets.query.PetStatusView;
import com.reactor.pets.service.PetManagerService;
import com.reactor.pets.util.PetAsciiArt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Statistics", description = "Endpoints for global statistics and leaderboards")
public class StatisticsController {

  private final PetManagerService petManagerService;

  @GetMapping("/statistics")
  @Operation(
      summary = "Get global statistics",
      description = "Retrieves global statistics across all pets")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics")
  public ResponseEntity<StatisticsResponse> getStatistics() {
    log.info("REST API: Getting global statistics");

    PetStatistics stats = petManagerService.getGlobalStatistics();

    StatisticsResponse response =
        StatisticsResponse.builder()
            .totalPetsCreated(stats.getTotalPetsCreated())
            .totalPetsDied(stats.getTotalPetsDied())
            .currentlyAlive(stats.getTotalPetsCreated() - stats.getTotalPetsDied())
            .averageLifespan(stats.getAverageLifespan())
            .longestLivedPetName(stats.getLongestLivedPetName())
            .longestLivedPetId(stats.getLongestLivedPetId())
            .longestLivedPetAge(stats.getLongestLivedPetAge())
            .stageDistribution(stats.getStageDistribution())
            .lastUpdated(stats.getLastUpdated())
            .build();

    return ResponseEntity.ok(response);
  }

  @GetMapping("/leaderboard")
  @Operation(
      summary = "Get leaderboard",
      description = "Retrieves the top 10 pets sorted by the specified metric")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved leaderboard")
  public ResponseEntity<LeaderboardResponse> getLeaderboard(
      @Parameter(description = "Leaderboard type (AGE, HAPPINESS, HEALTH)")
          @RequestParam(defaultValue = "AGE")
          String type,
      @Parameter(description = "Filter to show only alive pets")
          @RequestParam(defaultValue = "false")
          boolean aliveOnly) {
    log.info("REST API: Getting leaderboard for type: {}, aliveOnly: {}", type, aliveOnly);

    GetLeaderboardQuery.LeaderboardType leaderboardType;
    try {
      leaderboardType = GetLeaderboardQuery.LeaderboardType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Invalid leaderboard type: {}, defaulting to AGE", type);
      leaderboardType = GetLeaderboardQuery.LeaderboardType.AGE;
    }

    var pets = petManagerService.getLeaderboard(leaderboardType, aliveOnly);

    LeaderboardResponse response =
        LeaderboardResponse.builder()
            .type(leaderboardType.toString())
            .pets(
                pets.stream()
                    .map(this::mapToResponse)
                    .toList())
            .totalCount(pets.size())
            .build();

    return ResponseEntity.ok(response);
  }

  private PetStatusResponse mapToResponse(PetStatusView view) {
    return PetStatusResponse.builder()
        .petId(view.getPetId())
        .name(view.getName())
        .type(view.getType())
        .stage(view.getStage())
        .evolutionPath(view.getEvolutionPath())
        .isAlive(view.isAlive())
        .age(view.getAge())
        .totalTicks(view.getTotalTicks())
        .xpMultiplier(view.getXpMultiplier())
        .hunger(view.getHunger())
        .happiness(view.getHappiness())
        .health(view.getHealth())
        .lastUpdated(view.getLastUpdated())
        .asciiArt(PetAsciiArt.getArt(view.getType(), view.getStage()))
        .build();
  }
}
