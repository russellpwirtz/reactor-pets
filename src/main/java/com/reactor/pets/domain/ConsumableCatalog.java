package com.reactor.pets.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central catalog of all consumable items.
 * Provides consumable definitions with XP costs and effects.
 */
public final class ConsumableCatalog {

  private static final Map<ConsumableType, Consumable> CATALOG = new HashMap<>();

  static {
    // ========== FOOD CONSUMABLES ==========

    addConsumable(Consumable.createFood(
        ConsumableType.APPLE,
        "Apple",
        "A fresh apple that restores 30 hunger",
        50L,
        30));

    addConsumable(Consumable.createFood(
        ConsumableType.PIZZA,
        "Pizza",
        "A delicious pizza that restores 50 hunger",
        100L,
        50));

    addConsumable(Consumable.createFood(
        ConsumableType.GOURMET_MEAL,
        "Gourmet Meal",
        "An exquisite meal that restores 80 hunger",
        200L,
        80));

    // ========== MEDICINE CONSUMABLES ==========

    addConsumable(Consumable.createMedicine(
        ConsumableType.BASIC_MEDICINE,
        "Basic Medicine",
        "Basic medicine that restores 20 health but does NOT cure sickness",
        100L,
        20,
        false));

    addConsumable(Consumable.createMedicine(
        ConsumableType.ADVANCED_MEDICINE,
        "Advanced Medicine",
        "Advanced medicine that restores 40 health and CURES sickness",
        200L,
        40,
        true));

    // ========== TREAT CONSUMABLES ==========

    addConsumable(Consumable.createTreat(
        ConsumableType.COOKIE,
        "Cookie",
        "A tasty cookie that restores 20 happiness",
        75L,
        20,
        0));

    addConsumable(Consumable.createTreat(
        ConsumableType.PREMIUM_TOY,
        "Premium Toy",
        "A high-quality toy that restores 40 happiness and 10 health",
        150L,
        40,
        10));
  }

  private static void addConsumable(Consumable consumable) {
    consumable.validate();
    CATALOG.put(consumable.getType(), consumable);
  }

  /**
   * Gets a consumable definition by its type.
   *
   * @param type the consumable type to look up
   * @return Optional containing the consumable, or empty if not found
   */
  public static Optional<Consumable> getConsumable(ConsumableType type) {
    return Optional.ofNullable(CATALOG.get(type));
  }

  /**
   * Gets all consumable definitions.
   *
   * @return Map of all consumables
   */
  public static Map<ConsumableType, Consumable> getAllConsumables() {
    return new HashMap<>(CATALOG);
  }

  private ConsumableCatalog() {
    // Utility class
  }
}
