package com.reactor.pets.command;

import com.reactor.pets.domain.ConsumableType;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to add consumable items to player inventory.
 */
@Value
public class AddConsumableCommand {
  @TargetAggregateIdentifier
  String playerId;
  ConsumableType consumableType;
  int quantity; // Number of items to add
}
