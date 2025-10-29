package com.reactor.pets.command;

import com.reactor.pets.domain.EquipmentSlot;
import lombok.Value;

/**
 * Command to request equipping an item to a pet from the player's inventory.
 * This initiates the EquipmentSaga which coordinates the process.
 */
@Value
public class RequestEquipItemCommand {
  String playerId;
  String petId;
  String itemId;
  EquipmentSlot slot;
}
