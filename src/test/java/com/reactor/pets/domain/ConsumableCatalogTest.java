package com.reactor.pets.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsumableCatalog")
class ConsumableCatalogTest {

  @Test
  @DisplayName("should retrieve Apple consumable")
  void shouldRetrieveApple() {
    Optional<Consumable> apple = ConsumableCatalog.getConsumable(ConsumableType.APPLE);

    assertTrue(apple.isPresent());
    assertEquals("Apple", apple.get().getName());
    assertEquals(50L, apple.get().getXpCost());
    assertEquals(30, apple.get().getHungerRestore());
  }

  @Test
  @DisplayName("should retrieve Pizza consumable")
  void shouldRetrievePizza() {
    Optional<Consumable> pizza = ConsumableCatalog.getConsumable(ConsumableType.PIZZA);

    assertTrue(pizza.isPresent());
    assertEquals("Pizza", pizza.get().getName());
    assertEquals(100L, pizza.get().getXpCost());
    assertEquals(50, pizza.get().getHungerRestore());
  }

  @Test
  @DisplayName("should retrieve Gourmet Meal consumable")
  void shouldRetrieveGourmetMeal() {
    Optional<Consumable> meal = ConsumableCatalog.getConsumable(ConsumableType.GOURMET_MEAL);

    assertTrue(meal.isPresent());
    assertEquals("Gourmet Meal", meal.get().getName());
    assertEquals(200L, meal.get().getXpCost());
    assertEquals(80, meal.get().getHungerRestore());
  }

  @Test
  @DisplayName("should retrieve Basic Medicine consumable")
  void shouldRetrieveBasicMedicine() {
    Optional<Consumable> medicine = ConsumableCatalog.getConsumable(ConsumableType.BASIC_MEDICINE);

    assertTrue(medicine.isPresent());
    assertEquals("Basic Medicine", medicine.get().getName());
    assertEquals(100L, medicine.get().getXpCost());
    assertEquals(20, medicine.get().getHealthRestore());
    assertFalse(medicine.get().isCuresSickness());
  }

  @Test
  @DisplayName("should retrieve Advanced Medicine that cures sickness")
  void shouldRetrieveAdvancedMedicine() {
    Optional<Consumable> medicine = ConsumableCatalog.getConsumable(ConsumableType.ADVANCED_MEDICINE);

    assertTrue(medicine.isPresent());
    assertEquals("Advanced Medicine", medicine.get().getName());
    assertEquals(200L, medicine.get().getXpCost());
    assertEquals(40, medicine.get().getHealthRestore());
    assertTrue(medicine.get().isCuresSickness());
  }

  @Test
  @DisplayName("should retrieve Cookie consumable")
  void shouldRetrieveCookie() {
    Optional<Consumable> cookie = ConsumableCatalog.getConsumable(ConsumableType.COOKIE);

    assertTrue(cookie.isPresent());
    assertEquals("Cookie", cookie.get().getName());
    assertEquals(75L, cookie.get().getXpCost());
    assertEquals(20, cookie.get().getHappinessRestore());
  }

  @Test
  @DisplayName("should retrieve Premium Toy consumable")
  void shouldRetrievePremiumToy() {
    Optional<Consumable> toy = ConsumableCatalog.getConsumable(ConsumableType.PREMIUM_TOY);

    assertTrue(toy.isPresent());
    assertEquals("Premium Toy", toy.get().getName());
    assertEquals(150L, toy.get().getXpCost());
    assertEquals(40, toy.get().getHappinessRestore());
    assertEquals(10, toy.get().getHealthRestore());
  }

  @Test
  @DisplayName("should retrieve all consumables")
  void shouldRetrieveAllConsumables() {
    Map<ConsumableType, Consumable> allConsumables = ConsumableCatalog.getAllConsumables();

    assertNotNull(allConsumables);
    assertEquals(7, allConsumables.size());
    assertTrue(allConsumables.containsKey(ConsumableType.APPLE));
    assertTrue(allConsumables.containsKey(ConsumableType.PIZZA));
    assertTrue(allConsumables.containsKey(ConsumableType.GOURMET_MEAL));
    assertTrue(allConsumables.containsKey(ConsumableType.BASIC_MEDICINE));
    assertTrue(allConsumables.containsKey(ConsumableType.ADVANCED_MEDICINE));
    assertTrue(allConsumables.containsKey(ConsumableType.COOKIE));
    assertTrue(allConsumables.containsKey(ConsumableType.PREMIUM_TOY));
  }

  @Test
  @DisplayName("should validate all consumables in catalog")
  void shouldValidateAllConsumables() {
    Map<ConsumableType, Consumable> allConsumables = ConsumableCatalog.getAllConsumables();

    // All consumables should be valid (no exceptions)
    assertDoesNotThrow(() -> {
      for (Consumable consumable : allConsumables.values()) {
        consumable.validate();
      }
    });
  }

  @Test
  @DisplayName("should have consistent XP costs")
  void shouldHaveConsistentXpCosts() {
    // Food items should increase in cost with effectiveness
    Consumable apple = ConsumableCatalog.getConsumable(ConsumableType.APPLE).get();
    Consumable pizza = ConsumableCatalog.getConsumable(ConsumableType.PIZZA).get();
    Consumable gourmetMeal = ConsumableCatalog.getConsumable(ConsumableType.GOURMET_MEAL).get();

    assertTrue(apple.getXpCost() < pizza.getXpCost());
    assertTrue(pizza.getXpCost() < gourmetMeal.getXpCost());

    // Medicine items
    Consumable basicMedicine = ConsumableCatalog.getConsumable(ConsumableType.BASIC_MEDICINE).get();
    Consumable advancedMedicine = ConsumableCatalog.getConsumable(ConsumableType.ADVANCED_MEDICINE).get();

    assertTrue(basicMedicine.getXpCost() < advancedMedicine.getXpCost());
  }
}
