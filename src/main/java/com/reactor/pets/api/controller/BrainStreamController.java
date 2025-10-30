package com.reactor.pets.api.controller;

import com.reactor.pets.brain.model.CellState;
import com.reactor.pets.brain.service.PetBrainSimulator;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetStatusView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for streaming pet brain activity via Server-Sent Events (SSE).
 *
 * <p>Provides a reactive endpoint that streams brain cell state updates to connected clients.
 * The brain simulation only runs when clients are actively connected (subscription-based
 * lifecycle).
 */
@Slf4j
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Tag(name = "Brain Visualization", description = "Pet brain activity streaming endpoints")
public class BrainStreamController {

  private final PetBrainSimulator brainSimulator;
  private final QueryGateway queryGateway;

  /**
   * Stream brain activity for a pet via Server-Sent Events.
   *
   * <p>This endpoint returns a continuous stream of brain cell state updates. The brain
   * simulation starts when the first client connects and stops 30 seconds after the last client
   * disconnects.
   *
   * @param petId The ID of the pet whose brain activity to stream
   * @return SSE stream of cell state batches
   */
  @GetMapping(value = "/{petId}/brain/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(
      summary = "Stream pet brain activity",
      description =
          "Returns a Server-Sent Event stream of brain cell state updates. The brain simulation"
              + " starts on first connection and stops after 30s of no connections.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Brain activity stream started",
            content = @Content(schema = @Schema(implementation = CellState.class))),
        @ApiResponse(responseCode = "404", description = "Pet not found"),
        @ApiResponse(responseCode = "400", description = "Pet is not alive")
      })
  public Flux<ServerSentEvent<List<CellState>>> streamBrainActivity(
      @PathVariable String petId) {
    log.info("Client requesting brain stream for pet: {}", petId);

    // Query pet status to get current state
    return Mono.fromFuture(
            queryGateway.query(
                new GetPetStatusQuery(petId),
                PetStatusView.class))
        .flatMapMany(
            status -> {
              if (status == null) {
                log.warn("Pet not found: {}", petId);
                return Flux.error(
                    new IllegalArgumentException("Pet not found: " + petId));
              }

              if (!status.isAlive()) {
                log.warn("Cannot stream brain for dead pet: {}", petId);
                return Flux.error(
                    new IllegalStateException(
                        "Cannot stream brain activity for dead pet: " + petId));
              }

              log.info(
                  "Starting brain stream for pet {} (stage: {}, path: {})",
                  petId,
                  status.getStage(),
                  status.getEvolutionPath());

              // Subscribe to brain with current pet state
              // This will start simulation if needed and track subscriber count
              return brainSimulator
                  .subscribeToBrain(
                      petId,
                      status.getHunger(),
                      status.getHappiness(),
                      status.getHealth(),
                      status.getStage(),
                      status.getEvolutionPath())
                  .map(
                      cellStates ->
                          ServerSentEvent.<List<CellState>>builder()
                              .data(cellStates)
                              .build())
                  .doOnSubscribe(
                      sub -> log.info("Client subscribed to brain stream for pet: {}", petId))
                  .doOnCancel(() -> log.info("Client cancelled brain stream for pet: {}", petId))
                  .doOnComplete(
                      () -> log.info("Brain stream completed for pet: {}", petId))
                  .doOnError(
                      error -> log.error("Error in brain stream for pet {}: {}", petId, error.getMessage()));
            });
  }

  /**
   * Get current brain status (for debugging).
   *
   * @param petId The ID of the pet
   * @return Brain status information
   */
  @GetMapping("/{petId}/brain/status")
  @Operation(
      summary = "Get brain status",
      description = "Returns current brain simulation status for debugging purposes")
  public ResponseEntity<BrainStatusResponse> getBrainStatus(@PathVariable String petId) {
    log.debug("Brain status requested for pet: {}", petId);

    return Mono.fromFuture(
            queryGateway.query(
                new GetPetStatusQuery(petId),
                PetStatusView.class))
        .map(
            status -> {
              if (status == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                        new BrainStatusResponse(petId, false, "Pet not found", null, null));
              }

              if (!status.isAlive()) {
                return ResponseEntity.ok(
                    new BrainStatusResponse(
                        petId, false, "Pet is dead", status.getStage(), status.getEvolutionPath()));
              }

              return ResponseEntity.ok(
                  new BrainStatusResponse(
                      petId, true, "Ready", status.getStage(), status.getEvolutionPath()));
            })
        .block(); // Block for simple status endpoint
  }

  /** Response DTO for brain status endpoint. */
  private record BrainStatusResponse(
      String petId,
      boolean available,
      String message,
      com.reactor.pets.aggregate.PetStage stage,
      com.reactor.pets.aggregate.EvolutionPath evolutionPath) { }
}
