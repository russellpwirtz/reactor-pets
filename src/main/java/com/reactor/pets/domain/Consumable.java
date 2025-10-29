package com.reactor.pets.domain;

import lombok.Value;

/**
 * Represents a consumable item with its effects and properties.
 * Consumables are single-use items that provide immediate benefits.
 */
@Value
public class Consumable {
  ConsumableType type;
  String name;
  String description;
  long xpCost;
  int hungerRestore;      // Amount of hunger restored (0-100)
  int happinessRestore;   // Amount of happiness restored (0-100)
  int healthRestore;      // Amount of health restored (0-100)
  boolean curesSickness;  // Whether this consumable cures sickness

  /**
   * Creates a food consumable (restores hunger only).
   */
  public static Consumable createFood(
      ConsumableType type, String name, String description, long xpCost, int hungerRestore) {
    return new Consumable(type, name, description, xpCost, hungerRestore, 0, 0, false);
  }

  /**
   * Creates a treat consumable (restores happiness).
   */
  public static Consumable createTreat(
      ConsumableType type, String name, String description, long xpCost,
      int happinessRestore, int healthRestore) {
    return new Consumable(type, name, description, xpCost, 0, happinessRestore, healthRestore, false);
  }

  /**
   * Creates a medicine consumable (restores health, optionally cures sickness).
   */
  public static Consumable createMedicine(
      ConsumableType type, String name, String description, long xpCost,
      int healthRestore, boolean curesSickness) {
    return new Consumable(type, name, description, xpCost, 0, 0, healthRestore, curesSickness);
  }

  /**
   * Validates that the consumable is properly configured.
   *
   * @throws IllegalArgumentException if validation fails
   */
  public void validate() {
    if (type == null) {
      throw new IllegalArgumentException("Consumable type cannot be null");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Consumable name cannot be empty");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Consumable description cannot be empty");
    }
    if (xpCost < 0) {
      throw new IllegalArgumentException("XP cost cannot be negative");
    }
    if (hungerRestore < 0 || hungerRestore > 100) {
      throw new IllegalArgumentException("Hunger restore must be between 0-100");
    }
    if (happinessRestore < 0 || happinessRestore > 100) {
      throw new IllegalArgumentException("Happiness restore must be between 0-100");
    }
    if (healthRestore < 0 || healthRestore > 100) {
      throw new IllegalArgumentException("Health restore must be between 0-100");
    }
    // At least one effect must be positive
    if (hungerRestore == 0 && happinessRestore == 0 && healthRestore == 0 && !curesSickness) {
      throw new IllegalArgumentException("Consumable must have at least one effect");
    }
  }
}
