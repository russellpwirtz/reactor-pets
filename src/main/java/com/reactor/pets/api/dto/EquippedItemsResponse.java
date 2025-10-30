package com.reactor.pets.api.dto;

import com.reactor.pets.domain.EquipmentItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Response containing a pet's currently equipped items.
 */
@Value
@Builder
@Schema(description = "A pet's currently equipped items")
public class EquippedItemsResponse {
  @Schema(description = "Pet ID", example = "a5f37942-64f5-42c2-90ae-8780281b4336")
  String petId;

  @Schema(description = "Item equipped in the FOOD_BOWL slot", nullable = true)
  EquipmentItem foodBowl;

  @Schema(description = "Item equipped in the TOY slot", nullable = true)
  EquipmentItem toy;

  @Schema(description = "Item equipped in the ACCESSORY slot", nullable = true)
  EquipmentItem accessory;
}
