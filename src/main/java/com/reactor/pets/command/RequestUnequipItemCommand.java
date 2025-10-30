package com.reactor.pets.command;

import com.reactor.pets.domain.EquipmentSlot;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to request unequipping an item from a pet and returning it to inventory.
 * This command is handled by the Pet aggregate, which validates the request
 * and emits an ItemUnequipRequestedEvent to trigger the EquipmentSaga.
 */
@Value
public class RequestUnequipItemCommand {
  String playerId;
  @TargetAggregateIdentifier
  String petId;
  EquipmentSlot slot;
}
