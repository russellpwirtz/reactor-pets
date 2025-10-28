package com.reactor.pets.command;

import com.reactor.pets.aggregate.PetType;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class CreatePetCommand {
  @TargetAggregateIdentifier
  String petId;
  String name;
  PetType type;
}
