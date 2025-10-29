package com.reactor.pets.command;

import com.reactor.pets.domain.UpgradeType;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to purchase a permanent upgrade.
 */
@Value
public class PurchaseUpgradeCommand {
  @TargetAggregateIdentifier
  String playerId;
  UpgradeType upgradeType;
}
