package com.reactor.pets.event;

import com.reactor.pets.domain.EquipmentSlot;
import java.time.Instant;
import lombok.Value;

/**
 * Event emitted when a pet requests to unequip an item to return to inventory.
 * This triggers the EquipmentSaga to coordinate the cross-aggregate transaction.
 */
@Value
public class ItemUnequipRequestedEvent {
  String petId;
  String playerId;
  EquipmentSlot slot;
  Instant timestamp;
}
