package com.reactor.pets.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Factory for creating equipment items with their stat modifiers.
 * Works in conjunction with ShopCatalog to create purchasable equipment.
 */
public final class EquipmentCatalog {

  /**
   * Creates an equipment item based on a shop item definition.
   *
   * @param itemDefinition the shop item definition
   * @return a new EquipmentItem with appropriate modifiers
   * @throws IllegalArgumentException if the item is not an equipment type
   */
  public static EquipmentItem createEquipmentFromDefinition(ItemDefinition itemDefinition) {
    if (itemDefinition.getItemType() != ItemType.EQUIPMENT) {
      throw new IllegalArgumentException("Item must be of type EQUIPMENT");
    }

    String itemId = itemDefinition.getItemId();
    Map<StatModifier, Double> modifiers = new HashMap<>();

    // Configure modifiers based on item ID
    switch (itemId) {
      // Food Bowl items
      case "SLOW_FEEDER":
        modifiers.put(StatModifier.HUNGER_DECAY_RATE, -0.15); // -15% hunger decay
        break;

      case "NUTRIENT_BOWL":
        modifiers.put(StatModifier.HUNGER_DECAY_RATE, -0.25); // -25% hunger decay
        modifiers.put(StatModifier.FOOD_EFFICIENCY, 0.15); // +15% food efficiency
        break;

      case "AUTO_FEEDER":
        modifiers.put(StatModifier.HUNGER_DECAY_RATE, -0.40); // -40% hunger decay
        // Auto-feed logic will be implemented in Phase 7D
        break;

      // Toy items
      case "TOY_BOX":
        modifiers.put(StatModifier.HAPPINESS_DECAY_RATE, -0.15); // -15% happiness decay
        break;

      case "EXERCISE_WHEEL":
        modifiers.put(StatModifier.HAPPINESS_DECAY_RATE, -0.25); // -25% happiness decay
        modifiers.put(StatModifier.PLAY_EFFICIENCY, 0.15); // +15% play efficiency
        break;

      case "ENTERTAINMENT_SYSTEM":
        modifiers.put(StatModifier.HAPPINESS_DECAY_RATE, -0.40); // -40% happiness decay
        // Auto-play logic will be implemented in Phase 7D
        break;

      // Accessory items
      case "COZY_BED":
        modifiers.put(StatModifier.HEALTH_REGEN, 2.0); // +2 health per tick
        break;

      case "HEALTH_MONITOR":
        modifiers.put(StatModifier.HEALTH_DECAY_RATE, -0.30); // -30% health decay
        modifiers.put(StatModifier.HEALTH_REGEN, 1.0); // +1 health per tick
        break;

      case "XP_CHARM":
        // XP multiplier bonus will be applied separately
        // This item will boost the pet's XP multiplier
        modifiers.put(StatModifier.HEALTH_REGEN, 0.5); // Small bonus to justify the cost
        break;

      default:
        throw new IllegalArgumentException("Unknown equipment item: " + itemId);
    }

    return new EquipmentItem(
        UUID.randomUUID().toString(), // Generate unique instance ID
        itemDefinition.getName(),
        itemDefinition.getEquipmentSlot(),
        modifiers);
  }

  /**
   * Creates a permanent upgrade with its stat modifiers.
   *
   * @param upgradeType the type of upgrade
   * @return a new PermanentUpgrade with appropriate modifiers
   */
  public static PermanentUpgrade createUpgrade(UpgradeType upgradeType) {
    Map<StatModifier, Double> modifiers = new HashMap<>();

    switch (upgradeType) {
      case EFFICIENT_METABOLISM:
        modifiers.put(StatModifier.HUNGER_DECAY_RATE, -0.10); // -10% hunger decay
        break;

      case HAPPY_DISPOSITION:
        modifiers.put(StatModifier.HAPPINESS_DECAY_RATE, -0.10); // -10% happiness decay
        break;

      case STURDY_GENETICS:
        modifiers.put(StatModifier.HEALTH_DECAY_RATE, -0.10); // -10% health decay
        break;

      case INDUSTRIAL_KITCHEN:
        modifiers.put(StatModifier.FOOD_EFFICIENCY, 0.20); // +20% food efficiency
        break;

      case FAST_HATCHER:
        // Hatching time reduction will be handled in pet creation logic
        // No direct stat modifiers needed
        break;

      case MULTI_PET_LICENSE_I:
      case MULTI_PET_LICENSE_II:
      case MULTI_PET_LICENSE_III:
        // Pet limit increases are handled in game logic, not as stat modifiers
        // No direct stat modifiers needed
        break;

      default:
        throw new IllegalArgumentException("Unknown upgrade type: " + upgradeType);
    }

    return new PermanentUpgrade(upgradeType, modifiers);
  }

  private EquipmentCatalog() {
    // Utility class
  }
}
