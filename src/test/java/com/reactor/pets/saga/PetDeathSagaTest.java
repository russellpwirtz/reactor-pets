package com.reactor.pets.saga;

import static org.axonframework.test.matchers.Matchers.andNoMore;
import static org.axonframework.test.matchers.Matchers.exactSequenceOf;
import static org.axonframework.test.matchers.Matchers.messageWithPayload;
import static org.hamcrest.Matchers.any;

import com.reactor.pets.command.AddItemToInventoryCommand;
import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.domain.EquipmentSlot;
import com.reactor.pets.domain.StatModifier;
import com.reactor.pets.event.PetDiedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for PetDeathSaga. */
@DisplayName("PetDeathSaga")
class PetDeathSagaTest {

  private SagaTestFixture<PetDeathSaga> fixture;
  private static final String PET_ID = "test-pet-123";
  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    fixture = new SagaTestFixture<>(PetDeathSaga.class);
  }

  @Test
  @DisplayName("should start and end saga on PetDiedEvent with no equipped items")
  void shouldStartAndEndSagaOnPetDiedEventWithNoEquippedItems() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetDiedEvent(PET_ID, 10, 100, "Neglect", new ArrayList<>(), NOW))
        .expectActiveSagas(0); // Saga starts and ends immediately
  }

  @Test
  @DisplayName("should return equipped items to inventory when pet dies")
  void shouldReturnEquippedItemsToInventoryWhenPetDies() {
    // Create a list of equipped items
    List<EquipmentItem> equippedItems = new ArrayList<>();
    equippedItems.add(new EquipmentItem(
        "item-1",
        "BASIC_BOWL",
        EquipmentSlot.FOOD_BOWL,
        Map.of(StatModifier.FOOD_EFFICIENCY, 0.1)));
    equippedItems.add(new EquipmentItem(
        "item-2",
        "BASIC_TOY",
        EquipmentSlot.TOY,
        Map.of(StatModifier.PLAY_EFFICIENCY, 0.2)));

    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetDiedEvent(PET_ID, 10, 100, "Starvation", equippedItems, NOW))
        .expectActiveSagas(0) // Saga starts and ends immediately
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(AddItemToInventoryCommand.class)),
                messageWithPayload(any(AddItemToInventoryCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should handle pet death with single equipped item")
  void shouldHandlePetDeathWithSingleEquippedItem() {
    List<EquipmentItem> equippedItems = new ArrayList<>();
    equippedItems.add(new EquipmentItem(
        "item-1",
        "PREMIUM_COLLAR",
        EquipmentSlot.ACCESSORY,
        Map.of(StatModifier.HEALTH_REGEN, 0.15)));

    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetDiedEvent(PET_ID, 5, 50, "Old age", equippedItems, NOW))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(AddItemToInventoryCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should handle pet death with null equipped items")
  void shouldHandlePetDeathWithNullEquippedItems() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetDiedEvent(PET_ID, 0, 0, "Disease", null, NOW))
        .expectActiveSagas(0); // Should not crash on null
  }
}
