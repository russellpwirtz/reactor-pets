package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

/**
 * Event indicating that a pet became sick due to prolonged low health.
 */
@Value
public class PetBecameSickEvent {
  String petId;
  Instant timestamp;
}
