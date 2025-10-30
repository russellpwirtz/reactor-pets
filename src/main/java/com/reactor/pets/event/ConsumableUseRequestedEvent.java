package com.reactor.pets.event;

import com.reactor.pets.domain.ConsumableType;
import java.time.Instant;
import lombok.Value;

/**
 * Event emitted when a pet requests to use a consumable.
 * This triggers the ConsumableUsageSaga to validate inventory and apply effects.
 */
@Value
public class ConsumableUseRequestedEvent {
  String petId;
  String playerId;
  ConsumableType consumableType;
  Instant timestamp;
}
