package com.reactor.pets.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to make a pet mourn the death of another pet.
 * Reduces the pet's happiness by 10%.
 */
@Value
public class MournPetCommand {
  @TargetAggregateIdentifier
  String petId;

  String deceasedPetId;
  int happinessLoss;
}
