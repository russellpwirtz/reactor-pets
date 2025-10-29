package com.reactor.pets.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central catalog of all items available in the shop.
 * Provides item definitions with XP costs and details.
 */
public final class ShopCatalog {

  private static final Map<String, ItemDefinition> CATALOG = new HashMap<>();
  private static final Map<UpgradeType, ItemDefinition> UPGRADE_MAP = new HashMap<>();

  static {
    // ========== EQUIPMENT ITEMS ==========

    // Food Bowl Equipment (reduces hunger decay)
    addEquipment("SLOW_FEEDER", "Slow Feeder",
        "A specialized bowl that reduces hunger decay by 15%",
        200, EquipmentSlot.FOOD_BOWL);

    addEquipment("NUTRIENT_BOWL", "Nutrient Bowl",
        "An advanced bowl that reduces hunger decay by 25% and boosts food efficiency by 15%",
        300, EquipmentSlot.FOOD_BOWL);

    addEquipment("AUTO_FEEDER", "Auto-Feeder",
        "An automated feeding system that reduces hunger decay by 40% (requires consumables in Phase 7D)",
        500, EquipmentSlot.FOOD_BOWL);

    // Toy Equipment (boosts happiness)
    addEquipment("TOY_BOX", "Toy Box",
        "A collection of toys that reduces happiness decay by 15%",
        200, EquipmentSlot.TOY);

    addEquipment("EXERCISE_WHEEL", "Exercise Wheel",
        "A fun activity center that reduces happiness decay by 25% and boosts play efficiency by 15%",
        300, EquipmentSlot.TOY);

    addEquipment("ENTERTAINMENT_SYSTEM", "Entertainment System",
        "A full entertainment setup that reduces happiness decay by 40% (auto-plays in Phase 7D)",
        500, EquipmentSlot.TOY);

    // Accessory Equipment (health and XP bonuses)
    addEquipment("COZY_BED", "Cozy Bed",
        "A comfortable resting place that provides +2 health regeneration per tick",
        200, EquipmentSlot.ACCESSORY);

    addEquipment("HEALTH_MONITOR", "Health Monitor",
        "An advanced device that reduces health decay by 30% and provides +1 health regen",
        400, EquipmentSlot.ACCESSORY);

    addEquipment("XP_CHARM", "XP Charm",
        "A magical charm that boosts XP gain by 25% (XP multiplier bonus)",
        600, EquipmentSlot.ACCESSORY);

    // ========== PERMANENT UPGRADES ==========

    addUpgrade("EFFICIENT_METABOLISM", "Efficient Metabolism",
        "Reduces hunger decay by 10% for ALL pets permanently",
        200, UpgradeType.EFFICIENT_METABOLISM);

    addUpgrade("HAPPY_DISPOSITION", "Happy Disposition",
        "Reduces happiness decay by 10% for ALL pets permanently",
        150, UpgradeType.HAPPY_DISPOSITION);

    addUpgrade("STURDY_GENETICS", "Sturdy Genetics",
        "Reduces health decay by 10% for ALL pets permanently",
        150, UpgradeType.STURDY_GENETICS);

    addUpgrade("INDUSTRIAL_KITCHEN", "Industrial Kitchen",
        "Increases food efficiency by 20% for ALL pets permanently",
        500, UpgradeType.INDUSTRIAL_KITCHEN);

    addUpgrade("FAST_HATCHER", "Fast Hatcher",
        "Reduces egg hatching time by 25% for all future pets",
        250, UpgradeType.FAST_HATCHER);

    addUpgrade("MULTI_PET_LICENSE_I", "Multi-Pet License I",
        "Unlocks the ability to have 2 pets simultaneously",
        300, UpgradeType.MULTI_PET_LICENSE_I);

    addUpgrade("MULTI_PET_LICENSE_II", "Multi-Pet License II",
        "Unlocks the ability to have 3 pets simultaneously (requires License I)",
        600, UpgradeType.MULTI_PET_LICENSE_II);

    addUpgrade("MULTI_PET_LICENSE_III", "Multi-Pet License III",
        "Unlocks the ability to have 4 pets simultaneously (requires License II)",
        1000, UpgradeType.MULTI_PET_LICENSE_III);
  }

  private static void addEquipment(String id, String name, String description, long xpCost, EquipmentSlot slot) {
    ItemDefinition item = ItemDefinition.createEquipment(id, name, description, xpCost, slot);
    item.validate();
    CATALOG.put(id, item);
  }

  private static void addUpgrade(String id, String name, String description, long xpCost, UpgradeType upgradeType) {
    ItemDefinition item = ItemDefinition.createUpgrade(id, name, description, xpCost, upgradeType);
    item.validate();
    CATALOG.put(id, item);
    UPGRADE_MAP.put(upgradeType, item);
  }

  /**
   * Gets an item definition by its ID.
   *
   * @param itemId the item ID to look up
   * @return Optional containing the item definition, or empty if not found
   */
  public static Optional<ItemDefinition> getItem(String itemId) {
    return Optional.ofNullable(CATALOG.get(itemId));
  }

  /**
   * Gets all equipment items for a specific slot.
   *
   * @param slot the equipment slot to filter by
   * @return list of item definitions for that slot
   */
  public static List<ItemDefinition> getEquipmentBySlot(EquipmentSlot slot) {
    List<ItemDefinition> items = new ArrayList<>();
    for (ItemDefinition item : CATALOG.values()) {
      if (item.getItemType() == ItemType.EQUIPMENT && item.getEquipmentSlot() == slot) {
        items.add(item);
      }
    }
    return items;
  }

  /**
   * Gets all permanent upgrade definitions.
   *
   * @return list of all upgrade definitions
   */
  public static List<ItemDefinition> getAllUpgrades() {
    List<ItemDefinition> upgrades = new ArrayList<>();
    for (ItemDefinition item : CATALOG.values()) {
      if (item.getItemType() == ItemType.PERMANENT_UPGRADE) {
        upgrades.add(item);
      }
    }
    return upgrades;
  }

  /**
   * Gets all equipment item definitions.
   *
   * @return list of all equipment definitions
   */
  public static List<ItemDefinition> getAllEquipment() {
    List<ItemDefinition> equipment = new ArrayList<>();
    for (ItemDefinition item : CATALOG.values()) {
      if (item.getItemType() == ItemType.EQUIPMENT) {
        equipment.add(item);
      }
    }
    return equipment;
  }

  /**
   * Gets all item definitions in the catalog.
   *
   * @return list of all item definitions
   */
  public static List<ItemDefinition> getAllItems() {
    return new ArrayList<>(CATALOG.values());
  }

  /**
   * Gets the upgrade definition for a specific upgrade type.
   *
   * @param upgradeType the upgrade type to look up
   * @return Optional containing the upgrade definition, or empty if not found
   */
  public static Optional<ItemDefinition> getUpgrade(UpgradeType upgradeType) {
    return Optional.ofNullable(UPGRADE_MAP.get(upgradeType));
  }

  private ShopCatalog() {
    // Utility class
  }
}
