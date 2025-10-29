package com.reactor.pets.event;

import com.reactor.pets.domain.EquipmentItem;
import java.time.Instant;
import java.util.List;
import lombok.Value;

/**
 * Event published when a player's inventory is initialized with starter items.
 */
@Value
public class InventoryInitializedEvent {
  String playerId;
  List<EquipmentItem> starterItems;
  Instant timestamp;
}
