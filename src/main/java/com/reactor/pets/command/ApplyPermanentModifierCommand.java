package com.reactor.pets.command;

import com.reactor.pets.domain.PermanentUpgrade;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to apply a permanent upgrade modifier to a pet.
 * This command is dispatched by the PermanentUpgradeSaga when:
 * <ul>
 *   <li>A player purchases a permanent upgrade (applied to all existing alive pets)</li>
 *   <li>A new pet is created (all current permanent upgrades are applied)</li>
 * </ul>
 */
@Value
public class ApplyPermanentModifierCommand {
  @TargetAggregateIdentifier
  String petId;

  PermanentUpgrade upgrade;
}
