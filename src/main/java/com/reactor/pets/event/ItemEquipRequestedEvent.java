package com.reactor.pets.event;

import com.reactor.pets.domain.EquipmentSlot;
import java.time.Instant;
import lombok.Value;

/**
 * Event emitted when a pet requests to equip an item from inventory.
 * This triggers the EquipmentSaga to coordinate the cross-aggregate transaction.
 */
@Value
public class ItemEquipRequestedEvent {
  String petId;
  String playerId;
  String itemId;
  EquipmentSlot slot;
  Instant timestamp;
}
