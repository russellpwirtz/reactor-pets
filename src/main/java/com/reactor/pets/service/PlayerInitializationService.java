package com.reactor.pets.service;

import com.reactor.pets.command.InitializeInventoryCommand;
import com.reactor.pets.command.InitializePlayerCommand;
import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.domain.StarterEquipment;
import com.reactor.pets.query.GetInventoryQuery;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.InventoryView;
import com.reactor.pets.query.PlayerProgressionView;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service that initializes the player on application startup.
 * Ensures the player aggregate is created with starting XP.
 * Uses ApplicationStartedEvent (not ApplicationReadyEvent) because the app
 * has a CommandLineRunner with an infinite loop that prevents ApplicationReadyEvent from firing.
 */
@Service
@Slf4j
public class PlayerInitializationService {

  private static final String PLAYER_ID = "PLAYER_1"; // Single-player for now
  private static final String INVENTORY_ID = "PLAYER_1_INVENTORY"; // Inventory ID (must be different from PLAYER_ID)
  private static final long STARTING_XP = 100L; // Starting XP as per Phase 7C design

  private final CommandGateway commandGateway;
  private final QueryGateway queryGateway;

  // Constructor injection - log when bean is created
  public PlayerInitializationService(CommandGateway commandGateway, QueryGateway queryGateway) {
    this.commandGateway = commandGateway;
    this.queryGateway = queryGateway;
    log.info("=== PlayerInitializationService BEAN CREATED ===");
  }

  /**
   * Initialize the player on application startup if not already initialized.
   * This runs after the application context is fully initialized but before CommandLineRunner beans run.
   */
  @EventListener(ApplicationStartedEvent.class)
  public void initializePlayer() {
    log.info("=== ApplicationStartedEvent RECEIVED - initializePlayer() CALLED ===");
    log.info("Checking if player initialization is needed...");

    boolean playerExists = false;
    boolean inventoryExists = false;

    // Check if player is already initialized
    try {
      PlayerProgressionView existing = queryGateway
          .query(new GetPlayerProgressionQuery(PLAYER_ID), PlayerProgressionView.class)
          .join();

      if (existing != null) {
        playerExists = true;
        log.info("Player already initialized with {} XP", existing.getTotalXP());
      }
    } catch (Exception e) {
      log.debug("Player not found, will initialize: {}", e.getMessage());
    }

    // Check if inventory is already initialized
    try {
      InventoryView existing = queryGateway
          .query(new GetInventoryQuery(INVENTORY_ID), InventoryView.class)
          .join();

      if (existing != null) {
        inventoryExists = true;
        log.info("Inventory already initialized with {} items", existing.getItems().size());
      }
    } catch (Exception e) {
      log.debug("Inventory not found, will initialize: {}", e.getMessage());
    }

    // Initialize player if needed
    if (!playerExists) {
      try {
        log.info("Initializing player with {} starting XP", STARTING_XP);
        commandGateway.sendAndWait(new InitializePlayerCommand(PLAYER_ID, STARTING_XP));
        log.info("Player initialized successfully!");
      } catch (Exception e) {
        log.error("Failed to initialize player", e);
        throw new RuntimeException("Failed to initialize player", e);
      }
    }

    // Initialize inventory if needed (independent of player)
    if (!inventoryExists) {
      try {
        List<EquipmentItem> starterItems = new ArrayList<>();
        starterItems.add(StarterEquipment.createBasicBowl());
        starterItems.add(StarterEquipment.createSimpleToy());
        starterItems.add(StarterEquipment.createComfortBlanket());
        log.info("Initializing player inventory with {} starter items", starterItems.size());
        commandGateway.sendAndWait(new InitializeInventoryCommand(INVENTORY_ID, starterItems));
        log.info("Inventory initialized successfully!");
      } catch (Exception e) {
        log.error("Failed to initialize inventory", e);
        throw new RuntimeException("Failed to initialize inventory", e);
      }
    }

    if (playerExists && inventoryExists) {
      log.info("Player and inventory already initialized. Skipping initialization.");
    }
  }
}
