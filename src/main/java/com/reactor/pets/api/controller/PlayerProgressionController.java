package com.reactor.pets.api.controller;

import com.reactor.pets.api.dto.PlayerProgressionResponse;
import com.reactor.pets.api.dto.XPAnalyticsResponse;
import com.reactor.pets.query.GetAlivePetsQuery;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.PetStatusView;
import com.reactor.pets.query.PlayerProgressionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Player Progression", description = "Player progression and XP management")
public class PlayerProgressionController {

  private static final String PLAYER_ID = "PLAYER_1"; // Single-player for now

  private final QueryGateway queryGateway;

  @GetMapping("/progression")
  @Operation(summary = "Get player progression", description = "Returns current XP, lifetime XP, XP multiplier, and spending stats")
  public CompletableFuture<ResponseEntity<PlayerProgressionResponse>> getProgression() {
    log.debug("GET /api/progression");

    // Get player progression
    CompletableFuture<PlayerProgressionView> progressionFuture = queryGateway
        .query(new GetPlayerProgressionQuery(PLAYER_ID), PlayerProgressionView.class);

    // Get alive pets to calculate current multiplier
    CompletableFuture<List<PetStatusView>> petsFuture = queryGateway.query(new GetAlivePetsQuery(),
        ResponseTypes.multipleInstancesOf(PetStatusView.class));

    return progressionFuture.thenCombine(petsFuture, (progression, pets) -> {
      if (progression == null) {
        // Player not initialized yet, return zeros
        return ResponseEntity.ok(new PlayerProgressionResponse(
            PLAYER_ID,
            0L,
            0L,
            1.0,
            1.0,
            0L,
            java.util.Collections.emptySet()));
      }

      // Calculate current highest XP multiplier from alive pets
      double currentMultiplier = pets.stream()
          .mapToDouble(PetStatusView::getXpMultiplier)
          .max()
          .orElse(1.0);

      // Phase 7E: Use tracked totalXPSpent from projection
      long totalXPSpent = progression.getTotalXPSpent();

      // Phase 7E: Use tracked highest multiplier
      double highestMultiplier = progression.getHighestXPMultiplier();

      return ResponseEntity.ok(new PlayerProgressionResponse(
          progression.getPlayerId(),
          progression.getTotalXP(),
          progression.getLifetimeXPEarned(),
          currentMultiplier,
          highestMultiplier,
          totalXPSpent,
          progression.getPermanentUpgrades()));
    }).exceptionally(ex -> {
      log.error("Error fetching player progression", ex);
      return ResponseEntity.internalServerError().build();
    });
  }

  @GetMapping("/analytics/xp-rate")
  @Operation(
      summary = "Get XP analytics",
      description = "Returns XP earning rate, highest multiplier, and spending statistics")
  public CompletableFuture<ResponseEntity<XPAnalyticsResponse>> getXPAnalytics() {
    log.debug("GET /api/analytics/xp-rate");

    return queryGateway
        .query(new GetPlayerProgressionQuery(PLAYER_ID), PlayerProgressionView.class)
        .thenApply(progression -> {
          if (progression == null) {
            // Player not initialized yet, return zeros
            return ResponseEntity.ok(XPAnalyticsResponse.from(0L, 0L, 0L, 1.0, 0.0));
          }

          // TODO Phase 7E: Calculate XP per minute based on recent earning rate
          // For now, return 0.0 - will need to track XP earnings over time windows
          double xpPerMinute = 0.0;

          return ResponseEntity.ok(XPAnalyticsResponse.from(
              progression.getTotalXP(),
              progression.getLifetimeXPEarned(),
              progression.getTotalXPSpent(),
              progression.getHighestXPMultiplier(),
              xpPerMinute));
        })
        .exceptionally(ex -> {
          log.error("Error fetching XP analytics", ex);
          return ResponseEntity.internalServerError().build();
        });
  }
}
