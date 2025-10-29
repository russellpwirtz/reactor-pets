package com.reactor.pets.domain;

import java.util.HashMap;
import java.util.Map;
import lombok.Value;

/**
 * Represents a permanent upgrade that modifies global gameplay mechanics.
 * Unlike equipment, these upgrades are purchased once and apply forever.
 */
@Value
public class PermanentUpgrade {
  UpgradeType upgradeType;
  Map<StatModifier, Double> modifiers;

  /**
   * Validates that the upgrade is properly configured.
   *
   * @throws IllegalArgumentException if validation fails
   */
  public void validate() {
    if (upgradeType == null) {
      throw new IllegalArgumentException("Upgrade type cannot be null");
    }
    if (modifiers == null) {
      throw new IllegalArgumentException("Modifiers cannot be null");
    }
  }

  /**
   * Gets the modifier value for a specific stat, or 0.0 if not present.
   */
  public double getModifier(StatModifier modifier) {
    return modifiers.getOrDefault(modifier, 0.0);
  }

  /**
   * Creates a deep copy of the modifiers map to prevent external modification.
   */
  public Map<StatModifier, Double> getModifiersCopy() {
    return new HashMap<>(modifiers);
  }
}
