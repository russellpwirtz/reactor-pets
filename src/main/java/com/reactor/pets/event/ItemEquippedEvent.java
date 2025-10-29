package com.reactor.pets.event;

import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.domain.EquipmentSlot;
import java.time.Instant;
import lombok.Value;

/**
 * Event published when an item is successfully equipped to a pet.
 */
@Value
public class ItemEquippedEvent {
  String petId;
  EquipmentItem item;
  EquipmentSlot slot;
  Instant timestamp;
}
