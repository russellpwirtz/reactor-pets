package com.reactor.pets.event;

import com.reactor.pets.domain.UpgradeType;
import java.time.Instant;
import lombok.Value;

/**
 * Event indicating a permanent upgrade was purchased.
 */
@Value
public class UpgradePurchasedEvent {
  String playerId;
  UpgradeType upgradeType;
  Instant timestamp;
}
