package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class TimePassedEvent {
  String petId;
  int hungerIncrease;
  int happinessDecrease;
  int ageIncrease;
  long tickCount;
  Instant timestamp;
}
