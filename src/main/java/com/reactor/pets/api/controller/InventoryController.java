package com.reactor.pets.api.controller;

import com.reactor.pets.api.dto.EquipmentInventoryResponse;
import com.reactor.pets.query.GetInventoryQuery;
import com.reactor.pets.query.InventoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory Management", description = "Endpoints for managing player inventory")
public class InventoryController {

  private final QueryGateway queryGateway;

  @GetMapping("/equipment")
  @Operation(summary = "Get equipment inventory", description = "Retrieves the player's equipment inventory")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved equipment inventory"),
      @ApiResponse(responseCode = "404", description = "Inventory not found")
  })
  public CompletableFuture<ResponseEntity<EquipmentInventoryResponse>> getEquipmentInventory() {
    log.info("REST API: Getting equipment inventory");

    // Use single-player mode inventory ID
    GetInventoryQuery query = new GetInventoryQuery("PLAYER_1_INVENTORY");
    return queryGateway
        .query(query, InventoryView.class)
        .thenApply(view -> {
          EquipmentInventoryResponse response = EquipmentInventoryResponse.builder()
              .items(view.getItems())
              .build();
          return ResponseEntity.ok(response);
        })
        .exceptionally(ex -> {
          log.error("Failed to retrieve inventory", ex);
          return ResponseEntity.notFound().build();
        });
  }
}
