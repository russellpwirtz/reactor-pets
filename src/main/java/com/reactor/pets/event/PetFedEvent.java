package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class PetFedEvent {
  String petId;
  int hungerReduction;
  Instant timestamp;
}
