package com.reactor.pets.command;

import com.reactor.pets.domain.EquipmentSlot;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to unequip an item from a pet's equipment slot.
 */
@Value
public class UnequipItemCommand {
  @TargetAggregateIdentifier
  String petId;

  EquipmentSlot slot;
}
