package com.reactor.pets.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class PlayWithPetCommand {
  @TargetAggregateIdentifier
  String petId;
}
