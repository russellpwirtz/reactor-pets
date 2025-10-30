package com.reactor.pets.saga;

import com.reactor.pets.command.AddItemToInventoryCommand;
import com.reactor.pets.command.EquipItemCommand;
import com.reactor.pets.command.RemoveItemFromInventoryCommand;
import com.reactor.pets.command.UnequipItemCommand;
import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.event.ItemEquipRequestedEvent;
import com.reactor.pets.event.ItemEquippedEvent;
import com.reactor.pets.event.ItemUnequipRequestedEvent;
import com.reactor.pets.event.ItemUnequippedEvent;
import com.reactor.pets.query.GetInventoryItemQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saga that coordinates equipment operations across Pet and PlayerInventory
 * aggregates.
 * Handles equip and unequip flows with proper cross-aggregate coordination.
 */
@Saga
@Slf4j
public class EquipmentSaga {

  @Autowired
  private transient CommandGateway commandGateway;

  @Autowired
  private transient QueryGateway queryGateway;

  private String playerId;
  private String petId;
  private String itemId;
  private EquipmentItem item;

  /**
   * Handle item equip request event from Pet aggregate.
   * Step 1: Query the inventory for the item
   * Step 2: If found, remove from inventory
   * Step 3: Equip to pet
   */
  @StartSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(ItemEquipRequestedEvent event) {
    log.debug("EquipmentSaga: Item equip requested for item {} on pet {}",
        event.getItemId(), event.getPetId());

    // For single player mode, always use PLAYER_INVENTORY
    String inventoryId = "PLAYER_1_INVENTORY";

    this.playerId = event.getPlayerId();
    this.petId = event.getPetId();
    this.itemId = event.getItemId();

    // Associate saga with playerId as well
    SagaLifecycle.associateWith("playerId", playerId);

    try {
      // Query inventory for the item
      EquipmentItem inventoryItem = queryGateway
          .query(new GetInventoryItemQuery(inventoryId, itemId), EquipmentItem.class)
          .join();

      if (inventoryItem == null) {
        log.error("EquipmentSaga: Item {} not found in inventory", itemId);
        SagaLifecycle.end();
        return;
      }

      this.item = inventoryItem;

      // Remove item from inventory
      commandGateway.sendAndWait(new RemoveItemFromInventoryCommand(inventoryId, itemId));

      // Equip item to pet
      commandGateway.send(new EquipItemCommand(petId, item, event.getSlot()));

    } catch (Exception e) {
      log.error("EquipmentSaga: Error during equip process", e);
      SagaLifecycle.end();
    }
  }

  /**
   * Handle successful equipment.
   * Saga completes after item is equipped.
   */
  @EndSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(ItemEquippedEvent event) {
    log.debug("EquipmentSaga: Item {} successfully equipped to pet {}", event.getItem().getItemId(), event.getPetId());
  }

  /**
   * Handle item unequip request event from Pet aggregate.
   * Step 1: Unequip from pet
   * Step 2: Add back to inventory
   */
  @StartSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(ItemUnequipRequestedEvent event) {
    log.debug("EquipmentSaga: Item unequip requested for slot {} on pet {}",
        event.getSlot(), event.getPetId());

    this.playerId = event.getPlayerId();
    this.petId = event.getPetId();

    // Associate saga with playerId as well
    SagaLifecycle.associateWith("playerId", playerId);

    try {
      // Unequip item from pet (this will trigger ItemUnequippedEvent)
      commandGateway.send(new UnequipItemCommand(petId, event.getSlot()));
    } catch (Exception e) {
      log.error("EquipmentSaga: Error during unequip process", e);
      SagaLifecycle.end();
    }
  }

  /**
   * Handle item unequipped event.
   * Add the item back to inventory and end saga.
   */
  @EndSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(ItemUnequippedEvent event) {
    log.debug("EquipmentSaga: Item {} unequipped from pet {}, returning to inventory",
        event.getItem().getItemId(), event.getPetId());

    // Add item back to inventory (use fixed inventory ID for single player)
    String inventoryId = "PLAYER_1_INVENTORY";
    commandGateway.send(new AddItemToInventoryCommand(inventoryId, event.getItem()));
  }
}
