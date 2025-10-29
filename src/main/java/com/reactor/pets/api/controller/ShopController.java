package com.reactor.pets.api.controller;

import com.reactor.pets.command.PurchaseEquipmentCommand;
import com.reactor.pets.command.PurchaseUpgradeCommand;
import com.reactor.pets.domain.ItemDefinition;
import com.reactor.pets.domain.ItemType;
import com.reactor.pets.domain.UpgradeType;
import com.reactor.pets.query.GetPetCreationCostQuery;
import com.reactor.pets.query.GetShopItemQuery;
import com.reactor.pets.query.GetShopItemsQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shop", description = "Endpoints for browsing and purchasing shop items")
public class ShopController {

  private static final String PLAYER_ID = "PLAYER"; // Single-player for now

  private final CommandGateway commandGateway;
  private final QueryGateway queryGateway;

  @GetMapping("/items")
  @Operation(summary = "List shop items",
      description = "Retrieves all items available in the shop, optionally filtered by type")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved shop items")
  public CompletableFuture<ResponseEntity<List<ItemDefinition>>> getShopItems(
      @Parameter(description = "Filter by item type (EQUIPMENT or PERMANENT_UPGRADE)")
      @RequestParam(required = false) ItemType type) {
    log.info("REST API: Getting shop items with filter: {}", type);

    return queryGateway
        .query(new GetShopItemsQuery(type), ResponseTypes.multipleInstancesOf(ItemDefinition.class))
        .thenApply(ResponseEntity::ok);
  }

  @GetMapping("/items/{itemId}")
  @Operation(summary = "Get shop item",
      description = "Retrieves details of a specific shop item")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved item"),
      @ApiResponse(responseCode = "404", description = "Item not found")
  })
  public CompletableFuture<ResponseEntity<ItemDefinition>> getShopItem(
      @Parameter(description = "Item ID") @PathVariable String itemId) {
    log.info("REST API: Getting shop item: {}", itemId);

    return queryGateway
        .query(new GetShopItemQuery(itemId), ItemDefinition.class)
        .thenApply(item -> item != null
            ? ResponseEntity.ok(item)
            : ResponseEntity.notFound().build());
  }

  @GetMapping("/equipment")
  @Operation(summary = "List equipment items",
      description = "Retrieves all equipment items available in the shop")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved equipment items")
  public CompletableFuture<ResponseEntity<List<ItemDefinition>>> getEquipment() {
    return getShopItems(ItemType.EQUIPMENT);
  }

  @GetMapping("/upgrades")
  @Operation(summary = "List permanent upgrades",
      description = "Retrieves all permanent upgrades available in the shop")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved upgrades")
  public CompletableFuture<ResponseEntity<List<ItemDefinition>>> getUpgrades() {
    return getShopItems(ItemType.PERMANENT_UPGRADE);
  }

  @PostMapping("/purchase/equipment/{itemId}")
  @Operation(summary = "Purchase equipment",
      description = "Purchases an equipment item from the shop using XP")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Equipment purchased successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request or insufficient XP"),
      @ApiResponse(responseCode = "404", description = "Item not found")
  })
  public CompletableFuture<ResponseEntity<String>> purchaseEquipment(
      @Parameter(description = "Equipment item ID") @PathVariable String itemId) {
    log.info("REST API: Purchasing equipment: {}", itemId);

    return commandGateway
        .send(new PurchaseEquipmentCommand(PLAYER_ID, itemId))
        .thenApply(result -> ResponseEntity.ok("Equipment purchased successfully"))
        .exceptionally(ex -> {
          log.error("Failed to purchase equipment", ex);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body("Purchase failed: " + ex.getCause().getMessage());
        });
  }

  @PostMapping("/purchase/upgrade/{upgradeType}")
  @Operation(summary = "Purchase permanent upgrade",
      description = "Purchases a permanent upgrade from the shop using XP")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Upgrade purchased successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request or insufficient XP"),
      @ApiResponse(responseCode = "404", description = "Upgrade not found")
  })
  public CompletableFuture<ResponseEntity<String>> purchaseUpgrade(
      @Parameter(description = "Upgrade type") @PathVariable UpgradeType upgradeType) {
    log.info("REST API: Purchasing upgrade: {}", upgradeType);

    return commandGateway
        .send(new PurchaseUpgradeCommand(PLAYER_ID, upgradeType))
        .thenApply(result -> ResponseEntity.ok("Upgrade purchased successfully"))
        .exceptionally(ex -> {
          log.error("Failed to purchase upgrade", ex);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body("Purchase failed: " + ex.getCause().getMessage());
        });
  }

  @GetMapping("/pet-creation-cost")
  @Operation(summary = "Get pet creation cost",
      description = "Returns the XP cost for creating the next pet")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved cost")
  public CompletableFuture<ResponseEntity<Long>> getPetCreationCost() {
    log.info("REST API: Getting pet creation cost");

    return queryGateway
        .query(new GetPetCreationCostQuery(PLAYER_ID), Long.class)
        .thenApply(ResponseEntity::ok);
  }
}
