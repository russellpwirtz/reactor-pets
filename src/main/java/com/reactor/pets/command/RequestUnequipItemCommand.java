package com.reactor.pets.command;

import com.reactor.pets.domain.EquipmentSlot;
import lombok.Value;

/**
 * Command to request unequipping an item from a pet and returning it to inventory.
 * This initiates the EquipmentSaga which coordinates the process.
 */
@Value
public class RequestUnequipItemCommand {
  String playerId;
  String petId;
  EquipmentSlot slot;
}
