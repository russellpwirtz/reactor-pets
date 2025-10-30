package com.reactor.pets.api.controller;

import com.reactor.pets.api.dto.PurchaseResponse;
import com.reactor.pets.command.PurchaseEquipmentCommand;
import com.reactor.pets.command.PurchaseUpgradeCommand;
import com.reactor.pets.domain.ItemDefinition;
import com.reactor.pets.domain.ItemType;
import com.reactor.pets.domain.UpgradeType;
import com.reactor.pets.query.GetPetCreationCostQuery;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.GetShopItemQuery;
import com.reactor.pets.query.GetShopItemsQuery;
import com.reactor.pets.query.PlayerProgressionView;
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

    private static final String PLAYER_ID = "PLAYER_1"; // Single-player for now

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @GetMapping("/items")
    @Operation(summary = "List shop items", description = "Retrieves all items available in the shop, optionally filtered by type")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved shop items")
    public CompletableFuture<ResponseEntity<List<ItemDefinition>>> getShopItems(
            @Parameter(description = "Filter by item type (EQUIPMENT or PERMANENT_UPGRADE)") @RequestParam(required = false) ItemType type) {
        log.info("REST API: Getting shop items with filter: {}", type);

        return queryGateway
                .query(new GetShopItemsQuery(type), ResponseTypes.multipleInstancesOf(ItemDefinition.class))
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "Get shop item", description = "Retrieves details of a specific shop item")
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
    @Operation(summary = "List equipment items", description = "Retrieves all equipment items available in the shop")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved equipment items")
    public CompletableFuture<ResponseEntity<List<ItemDefinition>>> getEquipment() {
        return getShopItems(ItemType.EQUIPMENT);
    }

    @GetMapping("/upgrades")
    @Operation(summary = "List permanent upgrades", description = "Retrieves all permanent upgrades available in the shop")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved upgrades")
    public CompletableFuture<ResponseEntity<List<ItemDefinition>>> getUpgrades() {
        return getShopItems(ItemType.PERMANENT_UPGRADE);
    }

    @PostMapping("/purchase/equipment/{itemId}")
    @Operation(summary = "Purchase equipment", description = "Purchases an equipment item from the shop using XP")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Equipment purchased successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient XP"),
            @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public CompletableFuture<ResponseEntity<PurchaseResponse>> purchaseEquipment(
            @Parameter(description = "Equipment item ID") @PathVariable String itemId) {
        log.info("REST API: Purchasing equipment: {}", itemId);

        // First, get the item to know the cost
        return queryGateway
                .query(new GetShopItemQuery(itemId), ItemDefinition.class)
                .thenCompose(item -> {
                    if (item == null) {
                        return CompletableFuture.completedFuture(
                                ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(PurchaseResponse.builder()
                                                .success(false)
                                                .message("Item not found: " + itemId)
                                                .itemId(itemId)
                                                .build()));
                    }

                    // Purchase the equipment
                    return commandGateway
                            .send(new PurchaseEquipmentCommand(PLAYER_ID, itemId))
                            .thenCompose(result -> {
                                // Query updated player progression
                                return queryGateway
                                        .query(new GetPlayerProgressionQuery(PLAYER_ID), PlayerProgressionView.class)
                                        .thenApply(progression -> ResponseEntity.ok(
                                                PurchaseResponse.builder()
                                                        .success(true)
                                                        .message("Equipment purchased successfully")
                                                        .itemId(itemId)
                                                        .xpCost(item.getXpCost())
                                                        .remainingXP(progression.getTotalXP())
                                                        .build()));
                            })
                            .exceptionally(ex -> {
                                log.error("Failed to purchase equipment", ex);
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(PurchaseResponse.builder()
                                                .success(false)
                                                .message("Purchase failed: " + ex.getCause().getMessage())
                                                .itemId(itemId)
                                                .xpCost(item.getXpCost())
                                                .build());
                            });
                });
    }

    @PostMapping("/purchase/upgrade/{upgradeType}")
    @Operation(summary = "Purchase permanent upgrade", description = "Purchases a permanent upgrade from the shop using XP")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upgrade purchased successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient XP"),
            @ApiResponse(responseCode = "404", description = "Upgrade not found")
    })
    public CompletableFuture<ResponseEntity<PurchaseResponse>> purchaseUpgrade(
            @Parameter(description = "Upgrade type") @PathVariable UpgradeType upgradeType) {
        log.info("REST API: Purchasing upgrade: {}", upgradeType);

        // Get upgrade definition for cost
        return CompletableFuture.supplyAsync(() -> com.reactor.pets.domain.ShopCatalog.getUpgrade(upgradeType))
                .thenCompose(upgradeOpt -> {
                    if (upgradeOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(PurchaseResponse.builder()
                                                .success(false)
                                                .message("Upgrade not found: " + upgradeType)
                                                .itemId(upgradeType.name())
                                                .build()));
                    }

                    ItemDefinition upgrade = upgradeOpt.get();

                    // Purchase the upgrade
                    return commandGateway
                            .send(new PurchaseUpgradeCommand(PLAYER_ID, upgradeType))
                            .thenCompose(result -> {
                                // Query updated player progression
                                return queryGateway
                                        .query(new GetPlayerProgressionQuery(PLAYER_ID), PlayerProgressionView.class)
                                        .thenApply(progression -> ResponseEntity.ok(
                                                PurchaseResponse.builder()
                                                        .success(true)
                                                        .message("Upgrade purchased successfully")
                                                        .itemId(upgradeType.name())
                                                        .xpCost(upgrade.getXpCost())
                                                        .remainingXP(progression.getTotalXP())
                                                        .build()));
                                            })
                            .exceptionally(ex -> {
                                log.error("Failed to purchase upgrade", ex);
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(PurchaseResponse.builder()
                                                .success(false)
                                                .message("Purchase failed: " + ex.getCause().getMessage())
                                                .itemId(upgradeType.name())
                                                .xpCost(upgrade.getXpCost())
                                                .build());
                            });
                });
    }

    @GetMapping("/pet-creation-cost")
    @Operation(summary = "Get pet creation cost", description = "Returns the XP cost for creating the next pet")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved cost")
    public CompletableFuture<ResponseEntity<Long>> getPetCreationCost() {
        log.info("REST API: Getting pet creation cost");

        return queryGateway
                .query(new GetPetCreationCostQuery(PLAYER_ID), Long.class)
                .thenApply(ResponseEntity::ok);
    }
}
