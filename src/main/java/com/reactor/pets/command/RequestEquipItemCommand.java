package com.reactor.pets.command;

import com.reactor.pets.domain.EquipmentSlot;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to request equipping an item to a pet from the player's inventory.
 * This command is handled by the Pet aggregate, which validates the request
 * and emits an ItemEquipRequestedEvent to trigger the EquipmentSaga.
 */
@Value
public class RequestEquipItemCommand {
  String playerId;
  @TargetAggregateIdentifier
  String petId;
  String itemId;
  EquipmentSlot slot;
}
