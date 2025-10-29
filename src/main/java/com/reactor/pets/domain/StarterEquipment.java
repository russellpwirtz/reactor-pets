package com.reactor.pets.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Factory class for creating starter equipment items.
 * These items are provided to new players to help them get started.
 */
public final class StarterEquipment {

  public static EquipmentItem createBasicBowl() {
    Map<StatModifier, Double> modifiers = new HashMap<>();
    modifiers.put(StatModifier.FOOD_EFFICIENCY, 0.1); // +10% food effectiveness
    return new EquipmentItem(
        UUID.randomUUID().toString(),
        "Basic Bowl",
        EquipmentSlot.FOOD_BOWL,
        modifiers);
  }

  public static EquipmentItem createSimpleToy() {
    Map<StatModifier, Double> modifiers = new HashMap<>();
    modifiers.put(StatModifier.PLAY_EFFICIENCY, 0.1); // +10% play effectiveness
    return new EquipmentItem(
        UUID.randomUUID().toString(),
        "Simple Toy",
        EquipmentSlot.TOY,
        modifiers);
  }

  public static EquipmentItem createComfortBlanket() {
    Map<StatModifier, Double> modifiers = new HashMap<>();
    modifiers.put(StatModifier.HEALTH_REGEN, 1.0); // +1 health per tick
    return new EquipmentItem(
        UUID.randomUUID().toString(),
        "Comfort Blanket",
        EquipmentSlot.ACCESSORY,
        modifiers);
  }

  private StarterEquipment() {
    // Utility class
  }
}
