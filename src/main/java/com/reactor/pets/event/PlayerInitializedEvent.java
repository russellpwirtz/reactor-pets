package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class PlayerInitializedEvent {
  String playerId;
  long startingXP;
  Instant timestamp;
}
