package com.reactor.pets.event;

import com.reactor.pets.domain.ConsumableType;
import java.time.Instant;
import lombok.Value;

/**
 * Event indicating that consumable items were added to inventory.
 */
@Value
public class ConsumableAddedEvent {
  String playerId;
  ConsumableType consumableType;
  int quantity;
  int newQuantity; // Total quantity after adding
  Instant timestamp;
}
