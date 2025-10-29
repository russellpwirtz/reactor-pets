package com.reactor.pets.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to purchase equipment from the shop.
 * This command initiates the purchase saga which will:
 * 1. Validate XP cost via ShopCatalog
 * 2. Spend XP via SpendXPCommand
 * 3. Add item to inventory via AddItemToInventoryCommand
 */
@Value
public class PurchaseEquipmentCommand {
  @TargetAggregateIdentifier
  String playerId;
  String itemId; // Reference to ShopCatalog item
}
