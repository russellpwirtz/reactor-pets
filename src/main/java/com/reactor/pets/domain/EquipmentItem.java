package com.reactor.pets.domain;

import java.util.Map;
import lombok.Value;

/**
 * Represents an equipment item that can be equipped to a pet.
 * Each item has a name, slot type, and modifiers that affect pet stats.
 */
@Value
public class EquipmentItem {
  String itemId;
  String name;
  EquipmentSlot slot;
  Map<StatModifier, Double> modifiers;

  /**
   * Validates that the equipment item is properly configured.
   *
   * @throws IllegalArgumentException if validation fails
   */
  public void validate() {
    if (itemId == null || itemId.isBlank()) {
      throw new IllegalArgumentException("Item ID cannot be empty");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Item name cannot be empty");
    }
    if (slot == null) {
      throw new IllegalArgumentException("Equipment slot cannot be null");
    }
    if (modifiers == null || modifiers.isEmpty()) {
      throw new IllegalArgumentException("Equipment must have at least one modifier");
    }
  }

  /**
   * Gets the modifier value for a specific stat, or 0.0 if not present.
   */
  public double getModifier(StatModifier modifier) {
    return modifiers.getOrDefault(modifier, 0.0);
  }
}
