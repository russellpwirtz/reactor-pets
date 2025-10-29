package com.reactor.pets.api.controller;

import com.reactor.pets.api.dto.PlayerProgressionResponse;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.PlayerProgressionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Player Progression", description = "Player progression and XP management")
public class PlayerProgressionController {

  private static final String PLAYER_ID = "PLAYER"; // Single-player for now

  private final QueryGateway queryGateway;

  @GetMapping("/progression")
  @Operation(summary = "Get player progression", description = "Returns current XP, lifetime XP, total pets created")
  public CompletableFuture<ResponseEntity<PlayerProgressionResponse>> getProgression() {
    log.debug("GET /api/player/progression");

    return queryGateway
        .query(new GetPlayerProgressionQuery(PLAYER_ID), PlayerProgressionView.class)
        .thenApply(
            view -> {
              if (view == null) {
                // Player not initialized yet, return zeros
                return ResponseEntity.ok(new PlayerProgressionResponse(
                    PLAYER_ID,
                    0L,
                    0L,
                    0,
                    0));
              }
              return ResponseEntity.ok(new PlayerProgressionResponse(
                  view.getPlayerId(),
                  view.getTotalXP(),
                  view.getLifetimeXPEarned(),
                  view.getTotalPetsCreated(),
                  view.getPrestigeLevel()));
            })
        .exceptionally(
            ex -> {
              log.error("Error fetching player progression", ex);
              return ResponseEntity.internalServerError().build();
            });
  }
}
