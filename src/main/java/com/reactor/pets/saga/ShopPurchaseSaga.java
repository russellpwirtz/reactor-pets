package com.reactor.pets.saga;

import com.reactor.pets.command.AddItemToInventoryCommand;
import com.reactor.pets.command.PurchaseEquipmentCommand;
import com.reactor.pets.command.PurchaseUpgradeCommand;
import com.reactor.pets.command.SpendXPCommand;
import com.reactor.pets.domain.EquipmentCatalog;
import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.domain.ItemDefinition;
import com.reactor.pets.domain.ShopCatalog;
import com.reactor.pets.event.XPSpentEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saga that coordinates shop purchases across aggregates.
 * Handles:
 * 1. Equipment purchases: Validate item, spend XP, add to inventory
 * 2. Upgrade purchases: Validate upgrade, spend XP, apply upgrade
 */
@Saga
@Slf4j
public class ShopPurchaseSaga {

  private static final String INVENTORY_ID = "PLAYER_1_INVENTORY";

  @Autowired
  private transient CommandGateway commandGateway;

  private String playerId;
  private String itemId;
  private String purchasePurpose;
  private boolean isPurchaseComplete = false;

  /**
   * Handle equipment purchase request.
   * Step 1: Validate item exists and get XP cost
   * Step 2: Spend XP
   * Step 3: Create equipment and add to inventory
   */
  @StartSaga
  @SagaEventHandler(associationProperty = "playerId")
  public void handle(PurchaseEquipmentCommand command) {
    log.debug("ShopPurchaseSaga: Starting equipment purchase for item {} by player {}",
        command.getItemId(), command.getPlayerId());

    this.playerId = command.getPlayerId();
    this.itemId = command.getItemId();

    // Look up item in catalog
    ItemDefinition itemDef = ShopCatalog.getItem(itemId)
        .orElseThrow(() -> new IllegalArgumentException("Item not found in shop: " + itemId));

    // Validate it's equipment
    if (itemDef.getItemType() != com.reactor.pets.domain.ItemType.EQUIPMENT) {
      log.error("ShopPurchaseSaga: Item {} is not equipment", itemId);
      SagaLifecycle.end();
      return;
    }

    this.purchasePurpose = "Purchase: " + itemDef.getName();

    try {
      // Spend XP (this will trigger XPSpentEvent)
      log.debug("ShopPurchaseSaga: Spending {} XP for {}", itemDef.getXpCost(), itemDef.getName());
      commandGateway.sendAndWait(new SpendXPCommand(playerId, itemDef.getXpCost(), purchasePurpose));
    } catch (Exception e) {
      log.error("ShopPurchaseSaga: Failed to spend XP for equipment purchase", e);
      SagaLifecycle.end();
    }
  }

  /**
   * Handle upgrade purchase request.
   * Step 1: Validate upgrade and get XP cost
   * Step 2: Spend XP
   * Step 3: Apply upgrade to PlayerProgression
   */
  @StartSaga
  @SagaEventHandler(associationProperty = "playerId")
  public void handle(PurchaseUpgradeCommand command) {
    log.debug("ShopPurchaseSaga: Starting upgrade purchase for {} by player {}",
        command.getUpgradeType(), command.getPlayerId());

    this.playerId = command.getPlayerId();

    // Look up upgrade in catalog
    ItemDefinition upgradeDef = ShopCatalog.getUpgrade(command.getUpgradeType())
        .orElseThrow(() -> new IllegalArgumentException("Upgrade not found in shop: " + command.getUpgradeType()));

    this.purchasePurpose = "Upgrade: " + upgradeDef.getName();
    this.isPurchaseComplete = true; // Upgrades complete in single step

    try {
      // Spend XP first (this will trigger XPSpentEvent)
      log.debug("ShopPurchaseSaga: Spending {} XP for {}", upgradeDef.getXpCost(), upgradeDef.getName());
      commandGateway.sendAndWait(new SpendXPCommand(playerId, upgradeDef.getXpCost(), purchasePurpose));

      // The PurchaseUpgradeCommand will be processed by PlayerProgression aggregate
      // which already has the command handler that applies the upgrade
      log.info("ShopPurchaseSaga: Upgrade purchase completed for {}", command.getUpgradeType());
    } catch (Exception e) {
      log.error("ShopPurchaseSaga: Failed to purchase upgrade", e);
    } finally {
      SagaLifecycle.end();
    }
  }

  /**
   * Handle XP spent event for equipment purchases.
   * After XP is spent, create the equipment item and add to inventory.
   */
  @EndSaga
  @SagaEventHandler(associationProperty = "playerId")
  public void on(XPSpentEvent event) {
    // Only proceed if this is our purchase
    if (!event.getPurpose().equals(purchasePurpose)) {
      return;
    }

    // Skip if this was an upgrade purchase (already complete)
    if (isPurchaseComplete) {
      log.debug("ShopPurchaseSaga: Upgrade purchase already completed, ending saga");
      return;
    }

    log.debug("ShopPurchaseSaga: XP spent, creating equipment item {}", itemId);

    try {
      // Look up item definition again
      ItemDefinition itemDef = ShopCatalog.getItem(itemId)
          .orElseThrow(() -> new IllegalStateException("Item disappeared from catalog: " + itemId));

      // Create the equipment item with modifiers
      EquipmentItem newItem = EquipmentCatalog.createEquipmentFromDefinition(itemDef);

      // Add to inventory
      commandGateway.sendAndWait(new AddItemToInventoryCommand(INVENTORY_ID, newItem));

      log.info("ShopPurchaseSaga: Equipment purchase completed for {}", itemDef.getName());
    } catch (Exception e) {
      log.error("ShopPurchaseSaga: Failed to add equipment to inventory after XP spent", e);
      // Note: XP has been spent but item wasn't added - this is a partial failure
      // In production, we might need compensation logic here
    }
  }
}
