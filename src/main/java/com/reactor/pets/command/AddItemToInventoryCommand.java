package com.reactor.pets.command;

import com.reactor.pets.domain.EquipmentItem;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to add an item to the player's inventory (when unequipping or after pet death).
 */
@Value
public class AddItemToInventoryCommand {
  @TargetAggregateIdentifier
  String playerId;

  EquipmentItem item;
}
