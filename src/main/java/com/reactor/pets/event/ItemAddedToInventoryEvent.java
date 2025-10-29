package com.reactor.pets.event;

import com.reactor.pets.domain.EquipmentItem;
import java.time.Instant;
import lombok.Value;

/**
 * Event published when an item is added to the player's inventory.
 */
@Value
public class ItemAddedToInventoryEvent {
  String playerId;
  EquipmentItem item;
  Instant timestamp;
}
