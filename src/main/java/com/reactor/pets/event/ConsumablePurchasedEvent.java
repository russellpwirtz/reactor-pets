package com.reactor.pets.event;

import com.reactor.pets.domain.ConsumableType;
import java.time.Instant;
import lombok.Value;

/**
 * Event indicating a consumable was purchased from the shop.
 * Emitted by PlayerProgression aggregate after XP is spent.
 * Triggers ShopPurchaseSaga to add the consumable to inventory.
 */
@Value
public class ConsumablePurchasedEvent {
  String playerId;
  ConsumableType consumableType;
  int quantity;
  long xpSpent;
  Instant timestamp;
}
