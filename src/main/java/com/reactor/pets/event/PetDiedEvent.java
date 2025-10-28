package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class PetDiedEvent {
  String petId;
  int finalAge;
  int totalTicks;
  String causeOfDeath;
  Instant timestamp;
}
