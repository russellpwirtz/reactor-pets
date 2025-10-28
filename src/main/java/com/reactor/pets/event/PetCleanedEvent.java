package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class PetCleanedEvent {
  String petId;
  int healthIncrease;
  Instant timestamp;
}
