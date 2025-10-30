package com.reactor.pets.api.controller;

import com.reactor.pets.api.dto.PlayerProgressionResponse;
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

      // Calculate total XP spent (lifetime earned - current XP)
      long totalXPSpent = progression.getLifetimeXPEarned() - progression.getTotalXP();

      // For now, use current multiplier as highest (can be enhanced later to track
      // historical max)
      double highestMultiplier = Math.max(currentMultiplier, 1.0);

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
}
