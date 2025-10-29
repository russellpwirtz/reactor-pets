package com.reactor.pets.aggregate;

import com.reactor.pets.command.AddConsumableCommand;
import com.reactor.pets.command.AddItemToInventoryCommand;
import com.reactor.pets.command.InitializeInventoryCommand;
import com.reactor.pets.command.RemoveConsumableCommand;
import com.reactor.pets.command.RemoveItemFromInventoryCommand;
import com.reactor.pets.domain.ConsumableType;
import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.event.ConsumableAddedEvent;
import com.reactor.pets.event.ConsumableRemovedEvent;
import com.reactor.pets.event.InventoryInitializedEvent;
import com.reactor.pets.event.ItemAddedToInventoryEvent;
import com.reactor.pets.event.ItemRemovedFromInventoryEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

/**
 * Aggregate representing a player's inventory of equipment items and consumables.
 * Manages unequipped items that can be equipped to pets and consumables that can be used.
 */
@Aggregate
@NoArgsConstructor
public class PlayerInventory {

  @AggregateIdentifier
  private String playerId;

  // Map of itemId -> EquipmentItem for quick lookup
  private Map<String, EquipmentItem> items;

  // Map of ConsumableType -> quantity
  private Map<ConsumableType, Integer> consumables;

  @CommandHandler
  public PlayerInventory(InitializeInventoryCommand command) {
    // Validation
    if (command.getPlayerId() == null || command.getPlayerId().isBlank()) {
      throw new IllegalArgumentException("Player ID cannot be empty");
    }
    if (command.getStarterItems() == null) {
      throw new IllegalArgumentException("Starter items cannot be null");
    }

    // Apply event
    AggregateLifecycle.apply(
        new InventoryInitializedEvent(
            command.getPlayerId(),
            command.getStarterItems(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(AddItemToInventoryCommand command) {
    // Validation
    if (command.getItem() == null) {
      throw new IllegalArgumentException("Item cannot be null");
    }

    command.getItem().validate();

    // Apply event
    AggregateLifecycle.apply(
        new ItemAddedToInventoryEvent(
            command.getPlayerId(),
            command.getItem(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(RemoveItemFromInventoryCommand command) {
    // Validation
    if (command.getItemId() == null || command.getItemId().isBlank()) {
      throw new IllegalArgumentException("Item ID cannot be empty");
    }
    if (!items.containsKey(command.getItemId())) {
      throw new IllegalStateException("Item not found in inventory: " + command.getItemId());
    }

    // Apply event
    AggregateLifecycle.apply(
        new ItemRemovedFromInventoryEvent(
            command.getPlayerId(),
            command.getItemId(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(AddConsumableCommand command) {
    // Validation
    if (command.getConsumableType() == null) {
      throw new IllegalArgumentException("Consumable type cannot be null");
    }
    if (command.getQuantity() <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }

    // Calculate new quantity
    int currentQty = (this.consumables != null)
        ? this.consumables.getOrDefault(command.getConsumableType(), 0)
        : 0;
    int newQty = currentQty + command.getQuantity();

    // Apply event
    AggregateLifecycle.apply(
        new ConsumableAddedEvent(
            command.getPlayerId(),
            command.getConsumableType(),
            command.getQuantity(),
            newQty,
            Instant.now()));
  }

  @CommandHandler
  public void handle(RemoveConsumableCommand command) {
    // Validation
    if (command.getConsumableType() == null) {
      throw new IllegalArgumentException("Consumable type cannot be null");
    }
    if (command.getQuantity() <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }

    int currentQty = (this.consumables != null)
        ? this.consumables.getOrDefault(command.getConsumableType(), 0)
        : 0;

    if (currentQty < command.getQuantity()) {
      throw new IllegalStateException(
          String.format("Insufficient consumables. Required: %d, Available: %d",
              command.getQuantity(), currentQty));
    }

    // Calculate new quantity
    int newQty = currentQty - command.getQuantity();

    // Apply event
    AggregateLifecycle.apply(
        new ConsumableRemovedEvent(
            command.getPlayerId(),
            command.getConsumableType(),
            command.getQuantity(),
            newQty,
            Instant.now()));
  }

  @EventSourcingHandler
  public void on(InventoryInitializedEvent event) {
    this.playerId = event.getPlayerId();
    this.items = new HashMap<>();
    this.consumables = new HashMap<>();
    for (EquipmentItem item : event.getStarterItems()) {
      this.items.put(item.getItemId(), item);
    }
  }

  @EventSourcingHandler
  public void on(ItemAddedToInventoryEvent event) {
    this.items.put(event.getItem().getItemId(), event.getItem());
  }

  @EventSourcingHandler
  public void on(ItemRemovedFromInventoryEvent event) {
    this.items.remove(event.getItemId());
  }

  @EventSourcingHandler
  public void on(ConsumableAddedEvent event) {
    if (this.consumables == null) {
      this.consumables = new HashMap<>();
    }
    this.consumables.put(event.getConsumableType(), event.getNewQuantity());
  }

  @EventSourcingHandler
  public void on(ConsumableRemovedEvent event) {
    if (this.consumables == null) {
      this.consumables = new HashMap<>();
    }
    this.consumables.put(event.getConsumableType(), event.getNewQuantity());
  }
}
