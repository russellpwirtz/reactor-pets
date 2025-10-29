package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

/**
 * Event published when a pet loses happiness due to another pet's death.
 * This represents the mourning period where alive pets are affected by the loss.
 */
@Value
public class PetMournedEvent {
  String petId;
  String deceasedPetId;
  int happinessLoss;
  Instant timestamp;
}
