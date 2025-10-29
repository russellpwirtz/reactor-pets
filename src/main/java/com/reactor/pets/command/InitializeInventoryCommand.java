package com.reactor.pets.command;

import com.reactor.pets.domain.EquipmentItem;
import java.util.List;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to initialize a player's inventory with starter items.
 */
@Value
public class InitializeInventoryCommand {
  @TargetAggregateIdentifier
  String playerId;

  List<EquipmentItem> starterItems;
}
