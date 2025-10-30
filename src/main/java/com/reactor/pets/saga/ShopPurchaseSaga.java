package com.reactor.pets.saga;

import com.reactor.pets.command.AddItemToInventoryCommand;
import com.reactor.pets.domain.EquipmentCatalog;
import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.domain.ItemDefinition;
import com.reactor.pets.domain.ShopCatalog;
import com.reactor.pets.event.EquipmentPurchasedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saga that coordinates shop purchases across aggregates.
 * Listens to purchase events from PlayerProgression and coordinates inventory updates.
 */
@Saga
@Slf4j
public class ShopPurchaseSaga {

  private static final String INVENTORY_ID = "PLAYER_1_INVENTORY";

  @Autowired
  private transient CommandGateway commandGateway;

  /**
   * Handle equipment purchased event.
   * After XP is spent by PlayerProgression, add the equipment to inventory.
   */
  @StartSaga
  @EndSaga
  @SagaEventHandler(associationProperty = "playerId")
  public void on(EquipmentPurchasedEvent event) {
    log.debug("ShopPurchaseSaga: Equipment purchased, adding to inventory: {}",
        event.getItemId());

    String itemId = event.getItemId();

    try {
      // Look up item definition
      ItemDefinition itemDef = ShopCatalog.getItem(itemId)
          .orElseThrow(() -> new IllegalStateException("Item disappeared: " + itemId));

      // Create equipment item
      EquipmentItem newItem = EquipmentCatalog.createEquipmentFromDefinition(itemDef);

      // Add to inventory
      commandGateway.sendAndWait(
          new AddItemToInventoryCommand(INVENTORY_ID, newItem));

      log.info("ShopPurchaseSaga: Equipment added to inventory: {}", itemDef.getName());
    } catch (Exception e) {
      log.error("ShopPurchaseSaga: Failed to add equipment to inventory", e);
      // Note: XP already spent - potential compensation needed
    }
  }
}
