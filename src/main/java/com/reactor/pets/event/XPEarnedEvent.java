package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class XPEarnedEvent {
  String playerId;
  long xpAmount;
  long newTotalXP;
  long newLifetimeXP;
  String source;
  Instant timestamp;
}
