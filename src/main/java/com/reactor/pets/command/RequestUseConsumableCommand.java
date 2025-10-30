package com.reactor.pets.command;

import com.reactor.pets.domain.ConsumableType;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to request using a consumable item on a pet.
 * This command is handled by the Pet aggregate, which validates the request
 * and emits a ConsumableUseRequestedEvent to trigger the ConsumableUsageSaga.
 * The saga will then validate inventory and apply the consumable effects.
 */
@Value
public class RequestUseConsumableCommand {
  @TargetAggregateIdentifier
  String petId;
  ConsumableType consumableType;
  String playerId; // Needed to check inventory
}
