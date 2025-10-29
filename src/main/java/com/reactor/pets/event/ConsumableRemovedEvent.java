package com.reactor.pets.event;

import com.reactor.pets.domain.ConsumableType;
import java.time.Instant;
import lombok.Value;

/**
 * Event indicating that consumable items were removed from inventory.
 */
@Value
public class ConsumableRemovedEvent {
  String playerId;
  ConsumableType consumableType;
  int quantity;
  int newQuantity; // Total quantity after removing
  Instant timestamp;
}
