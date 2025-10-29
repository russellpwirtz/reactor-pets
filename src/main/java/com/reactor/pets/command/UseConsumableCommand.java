package com.reactor.pets.command;

import com.reactor.pets.domain.ConsumableType;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to use a consumable item on a pet.
 * This will trigger a saga to verify inventory and apply effects.
 */
@Value
public class UseConsumableCommand {
  @TargetAggregateIdentifier
  String petId;
  ConsumableType consumableType;
  String playerId; // Needed to check inventory
}
