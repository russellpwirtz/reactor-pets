package com.reactor.pets.command;

import com.reactor.pets.domain.ConsumableType;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to remove consumable items from player inventory.
 */
@Value
public class RemoveConsumableCommand {
  @TargetAggregateIdentifier
  String playerId;
  ConsumableType consumableType;
  int quantity; // Number of items to remove
}
