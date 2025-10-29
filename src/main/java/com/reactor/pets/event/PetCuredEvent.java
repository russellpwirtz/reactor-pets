package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

/**
 * Event indicating that a pet was cured from sickness.
 */
@Value
public class PetCuredEvent {
  String petId;
  Instant timestamp;
}
