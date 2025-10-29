package com.reactor.pets.domain;

/**
 * Represents the different equipment slots available for pets.
 * Slots unlock based on pet evolution stage:
 * - Baby: 1 slot
 * - Teen: 2 slots
 * - Adult: 3 slots
 */
public enum EquipmentSlot {
  FOOD_BOWL,
  TOY,
  ACCESSORY
}
