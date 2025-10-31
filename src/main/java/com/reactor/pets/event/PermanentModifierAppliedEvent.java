package com.reactor.pets.event;

import com.reactor.pets.domain.PermanentUpgrade;
import java.time.Instant;
import lombok.Value;

/**
 * Event published when a permanent upgrade modifier is applied to a pet.
 * This happens in two scenarios:
 * <ul>
 *   <li>When a player purchases a permanent upgrade (applied to all existing alive pets)</li>
 *   <li>When a new pet is created (all current permanent upgrades are applied)</li>
 * </ul>
 */
@Value
public class PermanentModifierAppliedEvent {
  String petId;
  PermanentUpgrade upgrade;
  Instant timestamp;
}
