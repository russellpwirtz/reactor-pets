package com.reactor.pets.api.controller;

import com.reactor.pets.api.dto.CreatePetRequest;
import com.reactor.pets.api.dto.PetHistoryResponse;
import com.reactor.pets.api.dto.PetStatusResponse;
import com.reactor.pets.command.CleanPetCommand;
import com.reactor.pets.command.FeedPetCommand;
import com.reactor.pets.command.PlayWithPetCommand;
import com.reactor.pets.query.GetAllPetsQuery;
import com.reactor.pets.query.GetPetHistoryQuery;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetEventDto;
import com.reactor.pets.query.PetStatusView;
import com.reactor.pets.service.PetCreationService;
import com.reactor.pets.util.PetAsciiArt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pet Management", description = "Endpoints for managing virtual pets")
public class PetController {

  private final CommandGateway commandGateway;
  private final QueryGateway queryGateway;
  private final PetCreationService petCreationService;

  @PostMapping
  @Operation(summary = "Create a new pet",
      description = "Creates a new virtual pet with the specified name and type. "
          + "First pet is FREE, subsequent pets cost XP (50, 100, 150, etc.)")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Pet created successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid request or insufficient XP")
  })
  public CompletableFuture<ResponseEntity<PetStatusResponse>> createPet(
      @Valid @RequestBody CreatePetRequest request) {
    log.info("REST API: Creating pet - name: {}, type: {}", request.getName(), request.getType());

    String petId = UUID.randomUUID().toString();

    return petCreationService
        .createPetWithCost(petId, request.getName(), request.getType())
        .thenCompose(
            createdPetId -> {
              // Query the newly created pet to return its status
              GetPetStatusQuery query = new GetPetStatusQuery(createdPetId);
              return queryGateway
                  .query(query, PetStatusView.class)
                  .thenApply(
                      view -> ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(view)));
            })
        .exceptionally(ex -> {
          log.error("Failed to create pet", ex);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        });
  }

  @GetMapping
  @Operation(summary = "List all pets", description = "Retrieves a list of all pets")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved pet list")
  public CompletableFuture<ResponseEntity<List<PetStatusResponse>>> getAllPets() {
    log.info("REST API: Getting all pets");

    return queryGateway
        .query(new GetAllPetsQuery(), ResponseTypes.multipleInstancesOf(PetStatusView.class))
        .thenApply(
            pets ->
                ResponseEntity.ok(
                    pets.stream().map(this::mapToResponse).toList()));
  }

  @GetMapping("/{petId}")
  @Operation(summary = "Get pet status", description = "Retrieves the current status of a specific pet")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Successfully retrieved pet status"),
    @ApiResponse(responseCode = "404", description = "Pet not found")
  })
  public CompletableFuture<ResponseEntity<PetStatusResponse>> getPetStatus(
      @Parameter(description = "Pet ID") @PathVariable String petId) {
    log.info("REST API: Getting status for pet: {}", petId);

    GetPetStatusQuery query = new GetPetStatusQuery(petId);
    return queryGateway
        .query(query, PetStatusView.class)
        .thenApply(view -> ResponseEntity.ok(mapToResponse(view)));
  }

  @PostMapping("/{petId}/feed")
  @Operation(summary = "Feed pet", description = "Feeds the pet to reduce hunger")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Pet fed successfully"),
    @ApiResponse(responseCode = "404", description = "Pet not found"),
    @ApiResponse(responseCode = "400", description = "Pet is dead")
  })
  public CompletableFuture<ResponseEntity<PetStatusResponse>> feedPet(
      @Parameter(description = "Pet ID") @PathVariable String petId) {
    log.info("REST API: Feeding pet: {}", petId);

    FeedPetCommand command = new FeedPetCommand(petId, 20);
    return commandGateway
        .send(command)
        .thenCompose(
            result -> {
              GetPetStatusQuery query = new GetPetStatusQuery(petId);
              return queryGateway
                  .query(query, PetStatusView.class)
                  .thenApply(view -> ResponseEntity.ok(mapToResponse(view)));
            });
  }

  @PostMapping("/{petId}/play")
  @Operation(summary = "Play with pet", description = "Play with the pet to increase happiness")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Played with pet successfully"),
    @ApiResponse(responseCode = "404", description = "Pet not found"),
    @ApiResponse(responseCode = "400", description = "Pet is dead")
  })
  public CompletableFuture<ResponseEntity<PetStatusResponse>> playWithPet(
      @Parameter(description = "Pet ID") @PathVariable String petId) {
    log.info("REST API: Playing with pet: {}", petId);

    PlayWithPetCommand command = new PlayWithPetCommand(petId);
    return commandGateway
        .send(command)
        .thenCompose(
            result -> {
              GetPetStatusQuery query = new GetPetStatusQuery(petId);
              return queryGateway
                  .query(query, PetStatusView.class)
                  .thenApply(view -> ResponseEntity.ok(mapToResponse(view)));
            });
  }

  @PostMapping("/{petId}/clean")
  @Operation(summary = "Clean pet", description = "Clean the pet to increase health")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Pet cleaned successfully"),
    @ApiResponse(responseCode = "404", description = "Pet not found"),
    @ApiResponse(responseCode = "400", description = "Pet is dead")
  })
  public CompletableFuture<ResponseEntity<PetStatusResponse>> cleanPet(
      @Parameter(description = "Pet ID") @PathVariable String petId) {
    log.info("REST API: Cleaning pet: {}", petId);

    CleanPetCommand command = new CleanPetCommand(petId);
    return commandGateway
        .send(command)
        .thenCompose(
            result -> {
              GetPetStatusQuery query = new GetPetStatusQuery(petId);
              return queryGateway
                  .query(query, PetStatusView.class)
                  .thenApply(view -> ResponseEntity.ok(mapToResponse(view)));
            });
  }

  @GetMapping("/{petId}/history")
  @Operation(summary = "Get pet event history", description = "Retrieves the event history for a specific pet")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Successfully retrieved pet history"),
    @ApiResponse(responseCode = "404", description = "Pet not found")
  })
  public CompletableFuture<ResponseEntity<PetHistoryResponse>> getPetHistory(
      @Parameter(description = "Pet ID") @PathVariable String petId,
      @Parameter(description = "Maximum number of events to return (max 50)")
          @RequestParam(defaultValue = "10")
          int limit) {
    log.info("REST API: Getting history for pet: {}, limit: {}", petId, limit);

    GetPetHistoryQuery query = new GetPetHistoryQuery(petId, limit);
    return queryGateway
        .query(query, ResponseTypes.multipleInstancesOf(PetEventDto.class))
        .thenApply(
            events -> {
              PetHistoryResponse response =
                  PetHistoryResponse.builder()
                      .petId(petId)
                      .events(
                          events.stream()
                              .map(
                                  e ->
                                      PetHistoryResponse.EventEntry.builder()
                                          .eventType(e.getEventType())
                                          .timestamp(e.getTimestamp())
                                          .payload(e.getDetails())
                                          .build())
                              .toList())
                      .totalEvents(events.size())
                      .build();
              return ResponseEntity.ok(response);
            });
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
