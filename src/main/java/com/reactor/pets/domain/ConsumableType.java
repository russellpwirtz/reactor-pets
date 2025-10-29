package com.reactor.pets.domain;

/**
 * Types of consumable items that can be used on pets.
 * Each consumable has different effects and XP costs.
 */
public enum ConsumableType {
  // Food consumables (restore hunger)
  /** Basic food item - restores 30 hunger */
  APPLE,

  /** Medium food item - restores 50 hunger */
  PIZZA,

  /** Premium food item - restores 80 hunger */
  GOURMET_MEAL,

  // Medicine consumables (affect health and sickness)
  /** Basic medicine - restores 20 health, does NOT cure sickness */
  BASIC_MEDICINE,

  /** Advanced medicine - restores 40 health, CURES sickness */
  ADVANCED_MEDICINE,

  // Treat consumables (restore happiness)
  /** Basic treat - restores 20 happiness */
  COOKIE,

  /** Premium treat - restores 40 happiness and minor health boost */
  PREMIUM_TOY
}
