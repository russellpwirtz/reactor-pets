package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class TimePassedEvent {
  String petId;
  int hungerIncrease;
  int happinessDecrease;
  int ageIncrease;
  long globalTick;
  double xpMultiplierChange; // Change in XP multiplier (0.0 if no change, +0.1 for milestone, etc.)
  double newXpMultiplier; // The new XP multiplier after this change
  Instant timestamp;
}
