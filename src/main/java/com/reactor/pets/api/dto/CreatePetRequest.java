package com.reactor.pets.api.dto;

import com.reactor.pets.aggregate.PetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new pet")
public class CreatePetRequest {

  @NotBlank(message = "Pet name is required")
  @Schema(description = "Name of the pet", example = "Fluffy", required = true)
  private String name;

  @NotNull(message = "Pet type is required")
  @Schema(
      description = "Type of the pet",
      example = "DOG",
      allowableValues = {"DOG", "CAT", "DRAGON"},
      required = true)
  private PetType type;
}
