package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class PetHealthDeterioratedEvent {
  String petId;
  int healthDecrease;
  String reason;
  Instant timestamp;
}
