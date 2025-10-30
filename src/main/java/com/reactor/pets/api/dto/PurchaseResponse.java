package com.reactor.pets.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for shop purchase operations")
public class PurchaseResponse {
  @Schema(description = "Whether the purchase was successful", example = "true")
  private boolean success;

  @Schema(description = "Success or error message", example = "Equipment purchased successfully")
  private String message;

  @Schema(description = "Item ID that was purchased", example = "GOLDEN_BOWL")
  private String itemId;

  @Schema(description = "XP cost of the purchase", example = "100")
  private Long xpCost;

  @Schema(description = "Remaining XP after purchase", example = "1150")
  private Long remainingXP;
}
