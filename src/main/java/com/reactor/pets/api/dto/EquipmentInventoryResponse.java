package com.reactor.pets.api.dto;

import com.reactor.pets.domain.EquipmentItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Response containing the player's equipment inventory.
 */
@Value
@Builder
@Schema(description = "Player's equipment inventory")
public class EquipmentInventoryResponse {
  @Schema(description = "List of equipment items in inventory")
  List<EquipmentItem> items;
}
