package com.reactor.pets.event;

import java.time.Instant;
import lombok.Value;

@Value
public class GlobalTimeCreatedEvent {
  String timeId;
  Instant timestamp;
}
