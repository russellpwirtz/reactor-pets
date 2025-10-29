package com.reactor.pets.api.dto;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current status and stats of a pet")
public class PetStatusResponse {

  @Schema(description = "Unique identifier of the pet", example = "123e4567-e89b-12d3-a456-426614174000")
  private String petId;

  @Schema(description = "Name of the pet", example = "Fluffy")
  private String name;

  @Schema(description = "Type of the pet", example = "DOG")
  private PetType type;

  @Schema(description = "Evolution stage of the pet", example = "BABY")
  private PetStage stage;

  @Schema(description = "Evolution path based on care quality", example = "HEALTHY")
  private EvolutionPath evolutionPath;

  @Schema(description = "Whether the pet is alive", example = "true")
  private boolean isAlive;

  @Schema(description = "Age of the pet in ticks", example = "25")
  private int age;

  @Schema(description = "Total ticks the pet has experienced", example = "50")
  private int totalTicks;

  @Schema(description = "Hunger level (0-100)", example = "45")
  private int hunger;

  @Schema(description = "Happiness level (0-100)", example = "75")
  private int happiness;

  @Schema(description = "Health level (0-100)", example = "90")
  private int health;

  @Schema(description = "Last updated timestamp")
  private Instant lastUpdated;

  @Schema(description = "ASCII art representation of the pet (optional)")
  private String asciiArt;
}
