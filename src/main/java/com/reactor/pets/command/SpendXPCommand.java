package com.reactor.pets.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class SpendXPCommand {
  @TargetAggregateIdentifier
  String playerId;
  long xpAmount;
  String purpose; // Description of what the XP is spent on
}
