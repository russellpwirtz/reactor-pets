package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class PetPlayedWithEvent {
  String petId;
  int happinessIncrease;
  int hungerIncrease;
  Instant timestamp;
}
