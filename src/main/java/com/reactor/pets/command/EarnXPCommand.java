package com.reactor.pets.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class EarnXPCommand {
  @TargetAggregateIdentifier
  String playerId;
  long xpAmount;
  String source; // Description of what earned the XP (e.g., "Fed pet", "Pet survived", "Pet evolved")
}
