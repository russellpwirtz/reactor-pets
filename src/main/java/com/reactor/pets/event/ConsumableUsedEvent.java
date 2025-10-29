package com.reactor.pets.event;

import com.reactor.pets.domain.ConsumableType;
import java.time.Instant;
import lombok.Value;

/**
 * Event indicating that a consumable was successfully used on a pet.
 */
@Value
public class ConsumableUsedEvent {
  String petId;
  ConsumableType consumableType;
  int hungerRestored;
  int happinessRestored;
  int healthRestored;
  boolean curedSickness;
  Instant timestamp;
}
