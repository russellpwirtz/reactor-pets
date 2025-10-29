package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class XPSpentEvent {
  String playerId;
  long xpAmount;
  long newTotalXP;
  String purpose;
  Instant timestamp;
}
