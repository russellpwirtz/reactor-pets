package com.reactor.pets.domain;

import lombok.Value;

/**
 * Defines an item available for purchase in the shop.
 * Acts as a catalog entry with pricing and item details.
 */
@Value
public class ItemDefinition {
  String itemId;
  String name;
  String description;
  ItemType itemType;
  long xpCost;

  // For EQUIPMENT type
  EquipmentSlot equipmentSlot;

  // For PERMANENT_UPGRADE type
  UpgradeType upgradeType;

  /**
   * Validates that the item definition is properly configured.
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
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Item description cannot be empty");
    }
    if (itemType == null) {
      throw new IllegalArgumentException("Item type cannot be null");
    }
    if (xpCost < 0) {
      throw new IllegalArgumentException("XP cost cannot be negative");
    }

    // Type-specific validation
    if (itemType == ItemType.EQUIPMENT && equipmentSlot == null) {
      throw new IllegalArgumentException("Equipment items must have an equipment slot");
    }
    if (itemType == ItemType.PERMANENT_UPGRADE && upgradeType == null) {
      throw new IllegalArgumentException("Permanent upgrades must have an upgrade type");
    }
  }

  /**
   * Creates an equipment item definition.
   */
  public static ItemDefinition createEquipment(
      String itemId,
      String name,
      String description,
      long xpCost,
      EquipmentSlot slot) {
    return new ItemDefinition(itemId, name, description, ItemType.EQUIPMENT, xpCost, slot, null);
  }

  /**
   * Creates a permanent upgrade item definition.
   */
  public static ItemDefinition createUpgrade(
      String itemId,
      String name,
      String description,
      long xpCost,
      UpgradeType upgradeType) {
    return new ItemDefinition(itemId, name, description, ItemType.PERMANENT_UPGRADE, xpCost, null, upgradeType);
  }
}
