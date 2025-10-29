package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

/**
 * Event published when an item is removed from the player's inventory.
 */
@Value
public class ItemRemovedFromInventoryEvent {
  String playerId;
  String itemId;
  Instant timestamp;
}
