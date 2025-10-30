package com.reactor.pets.api.dto;

import com.reactor.pets.domain.EquipmentSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

/**
 * Request to equip an item to a pet.
 */
@Value
@Schema(description = "Request to equip an item to a pet")
public class EquipItemRequest {
  @NotNull
  @Schema(description = "Equipment slot", example = "FOOD_BOWL")
  EquipmentSlot slot;

  @NotBlank
  @Schema(description = "Item ID", example = "basic_bowl_001")
  String itemId;
}
