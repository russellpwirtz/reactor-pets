package com.reactor.pets.api.dto;

import com.reactor.pets.domain.EquipmentSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

/**
 * Request to unequip an item from a pet.
 */
@Value
@Schema(description = "Request to unequip an item from a pet")
public class UnequipItemRequest {
  @NotNull
  @Schema(description = "Equipment slot", example = "FOOD_BOWL")
  EquipmentSlot slot;
}
