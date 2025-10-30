package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

/**
 * Event indicating equipment was purchased from the shop.
 * Emitted by PlayerProgression aggregate after XP is spent.
 * Triggers ShopPurchaseSaga to add the item to inventory.
 */
@Value
public class EquipmentPurchasedEvent {
  String playerId;
  String itemId;           // Shop catalog item ID
  long xpSpent;
  String itemName;         // For logging
  Instant timestamp;
}
