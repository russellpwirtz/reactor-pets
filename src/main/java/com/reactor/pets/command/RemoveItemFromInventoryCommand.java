package com.reactor.pets.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to remove an item from the player's inventory (when equipping).
 */
@Value
public class RemoveItemFromInventoryCommand {
  @TargetAggregateIdentifier
  String playerId;

  String itemId;
}
