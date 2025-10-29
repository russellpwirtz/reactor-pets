package com.reactor.pets.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Consumable Domain Model")
class ConsumableTest {

  @Test
  @DisplayName("should create food consumable with correct properties")
  void shouldCreateFoodConsumable() {
    Consumable apple = Consumable.createFood(
        ConsumableType.APPLE,
        "Apple",
        "A fresh apple",
        50L,
        30);

    assertEquals(ConsumableType.APPLE, apple.getType());
    assertEquals("Apple", apple.getName());
    assertEquals("A fresh apple", apple.getDescription());
    assertEquals(50L, apple.getXpCost());
    assertEquals(30, apple.getHungerRestore());
    assertEquals(0, apple.getHappinessRestore());
    assertEquals(0, apple.getHealthRestore());
    assertFalse(apple.isCuresSickness());
  }

  @Test
  @DisplayName("should create treat consumable with correct properties")
  void shouldCreateTreatConsumable() {
    Consumable cookie = Consumable.createTreat(
        ConsumableType.COOKIE,
        "Cookie",
        "A tasty cookie",
        75L,
        20,
        0);

    assertEquals(ConsumableType.COOKIE, cookie.getType());
    assertEquals("Cookie", cookie.getName());
    assertEquals(20, cookie.getHappinessRestore());
    assertEquals(0, cookie.getHealthRestore());
    assertFalse(cookie.isCuresSickness());
  }

  @Test
  @DisplayName("should create medicine consumable with cure property")
  void shouldCreateMedicineConsumable() {
    Consumable advancedMedicine = Consumable.createMedicine(
        ConsumableType.ADVANCED_MEDICINE,
        "Advanced Medicine",
        "Cures sickness",
        200L,
        40,
        true);

    assertEquals(ConsumableType.ADVANCED_MEDICINE, advancedMedicine.getType());
    assertEquals(40, advancedMedicine.getHealthRestore());
    assertTrue(advancedMedicine.isCuresSickness());
    assertEquals(0, advancedMedicine.getHungerRestore());
    assertEquals(0, advancedMedicine.getHappinessRestore());
  }

  @Test
  @DisplayName("should validate consumable successfully")
  void shouldValidateConsumable() {
    Consumable validConsumable = Consumable.createFood(
        ConsumableType.APPLE,
        "Apple",
        "A fresh apple",
        50L,
        30);

    assertDoesNotThrow(validConsumable::validate);
  }

  @Test
  @DisplayName("should reject consumable with null type")
  void shouldRejectNullType() {
    Consumable invalidConsumable = Consumable.createFood(
        null,
        "Apple",
        "A fresh apple",
        50L,
        30);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        invalidConsumable::validate);
    assertTrue(exception.getMessage().contains("type cannot be null"));
  }

  @Test
  @DisplayName("should reject consumable with empty name")
  void shouldRejectEmptyName() {
    Consumable invalidConsumable = Consumable.createFood(
        ConsumableType.APPLE,
        "",
        "A fresh apple",
        50L,
        30);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        invalidConsumable::validate);
    assertTrue(exception.getMessage().contains("name cannot be empty"));
  }

  @Test
  @DisplayName("should reject consumable with empty description")
  void shouldRejectEmptyDescription() {
    Consumable invalidConsumable = Consumable.createFood(
        ConsumableType.APPLE,
        "Apple",
        "",
        50L,
        30);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        invalidConsumable::validate);
    assertTrue(exception.getMessage().contains("description cannot be empty"));
  }

  @Test
  @DisplayName("should reject consumable with negative XP cost")
  void shouldRejectNegativeXpCost() {
    Consumable invalidConsumable = Consumable.createFood(
        ConsumableType.APPLE,
        "Apple",
        "A fresh apple",
        -50L,
        30);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        invalidConsumable::validate);
    assertTrue(exception.getMessage().contains("XP cost cannot be negative"));
  }

  @Test
  @DisplayName("should reject consumable with invalid hunger restore")
  void shouldRejectInvalidHungerRestore() {
    Consumable invalidConsumable = Consumable.createFood(
        ConsumableType.APPLE,
        "Apple",
        "A fresh apple",
        50L,
        150); // > 100

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        invalidConsumable::validate);
    assertTrue(exception.getMessage().contains("Hunger restore must be between 0-100"));
  }

  @Test
  @DisplayName("should reject consumable with invalid happiness restore")
  void shouldRejectInvalidHappinessRestore() {
    Consumable invalidConsumable = Consumable.createTreat(
        ConsumableType.COOKIE,
        "Cookie",
        "A cookie",
        75L,
        150, // > 100
        0);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        invalidConsumable::validate);
    assertTrue(exception.getMessage().contains("Happiness restore must be between 0-100"));
  }

  @Test
  @DisplayName("should reject consumable with invalid health restore")
  void shouldRejectInvalidHealthRestore() {
    Consumable invalidConsumable = Consumable.createMedicine(
        ConsumableType.BASIC_MEDICINE,
        "Medicine",
        "Basic medicine",
        100L,
        150, // > 100
        false);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        invalidConsumable::validate);
    assertTrue(exception.getMessage().contains("Health restore must be between 0-100"));
  }

  @Test
  @DisplayName("should create treat with both happiness and health restore")
  void shouldCreateTreatWithMultipleEffects() {
    Consumable premiumToy = Consumable.createTreat(
        ConsumableType.PREMIUM_TOY,
        "Premium Toy",
        "A fancy toy",
        150L,
        40,
        10);

    assertEquals(40, premiumToy.getHappinessRestore());
    assertEquals(10, premiumToy.getHealthRestore());
    assertDoesNotThrow(premiumToy::validate);
  }
}
