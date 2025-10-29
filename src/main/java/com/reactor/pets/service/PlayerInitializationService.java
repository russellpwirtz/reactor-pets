package com.reactor.pets.service;

import com.reactor.pets.command.InitializeInventoryCommand;
import com.reactor.pets.command.InitializePlayerCommand;
import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.domain.StarterEquipment;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.PlayerProgressionView;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service that initializes the player on application startup.
 * Ensures the player aggregate is created with starting XP.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlayerInitializationService {

  private static final String PLAYER_ID = "PLAYER"; // Single-player for now
  private static final String INVENTORY_ID = "PLAYER_INVENTORY"; // Inventory ID (must be different from PLAYER_ID)
  private static final long STARTING_XP = 100L; // Starting XP as per Phase 7C design

  private final CommandGateway commandGateway;
  private final QueryGateway queryGateway;

  /**
   * Initialize the player on application startup if not already initialized.
   * This runs after the application context is fully initialized.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void initializePlayer() {
    log.info("Checking if player initialization is needed...");

    try {
      // Check if player is already initialized
      PlayerProgressionView existing = queryGateway
          .query(new GetPlayerProgressionQuery(PLAYER_ID), PlayerProgressionView.class)
          .join();

      if (existing != null) {
        log.info("Player already initialized with {} XP. Skipping initialization.", existing.getTotalXP());
        return;
      }
    } catch (Exception e) {
      log.debug("Player not found, will initialize: {}", e.getMessage());
    }

    // Initialize the player
    try {
      log.info("Initializing player with {} starting XP", STARTING_XP);
      commandGateway.sendAndWait(new InitializePlayerCommand(PLAYER_ID, STARTING_XP));
      log.info("Player initialized successfully!");

      // Initialize inventory with starter equipment
      List<EquipmentItem> starterItems = new ArrayList<>();
      starterItems.add(StarterEquipment.createBasicBowl());
      starterItems.add(StarterEquipment.createSimpleToy());
      starterItems.add(StarterEquipment.createComfortBlanket());
      log.info("Initializing player inventory with {} starter items", starterItems.size());
      commandGateway.sendAndWait(new InitializeInventoryCommand(INVENTORY_ID, starterItems));
      log.info("Inventory initialized successfully!");
    } catch (Exception e) {
      log.error("Failed to initialize player", e);
      throw new RuntimeException("Failed to initialize player", e);
    }
  }
}
