package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class GlobalTimeAdvancedEvent {
  String timeId;
  long newGlobalTick;
  Instant timestamp;
}
