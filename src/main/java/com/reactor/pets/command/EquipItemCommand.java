package com.reactor.pets.command;

import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.domain.EquipmentSlot;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to equip an item to a pet's equipment slot.
 * This command is initiated by the saga after validating inventory.
 */
@Value
public class EquipItemCommand {
  @TargetAggregateIdentifier
  String petId;

  EquipmentItem item;
  EquipmentSlot slot;
}
