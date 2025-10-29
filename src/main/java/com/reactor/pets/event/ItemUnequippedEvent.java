package com.reactor.pets.event;

import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.domain.EquipmentSlot;
import java.time.Instant;
import lombok.Value;

/**
 * Event published when an item is unequipped from a pet.
 */
@Value
public class ItemUnequippedEvent {
  String petId;
  EquipmentItem item;
  EquipmentSlot slot;
  Instant timestamp;
}
