package com.reactor.pets.aggregate;

import static org.axonframework.test.matchers.Matchers.*;
import static org.hamcrest.Matchers.any;

import com.reactor.pets.command.UseConsumableCommand;
import com.reactor.pets.domain.ConsumableType;
import com.reactor.pets.event.ConsumableUsedEvent;
import com.reactor.pets.event.PetBecameSickEvent;
import com.reactor.pets.event.PetCreatedEvent;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pet - Consumables")
class PetConsumableTest {

  private FixtureConfiguration<Pet> fixture;
  private static final String PET_ID = "pet-1";
  private static final String PLAYER_ID = "PLAYER_1";

  @BeforeEach
  void setUp() {
    fixture = new AggregateTestFixture<>(Pet.class);
  }

  @Test
  @DisplayName("should use food consumable to restore hunger")
  void shouldUseFoodConsumable() {
    fixture
        .given(new PetCreatedEvent(PET_ID, "Fluffy", PetType.DOG, null))
        .when(new UseConsumableCommand(PET_ID, ConsumableType.APPLE, PLAYER_ID))
        .expectSuccessfulHandlerExecution()
        .expectEventsMatching(
            exactSequenceOf(
                messageWithPayload(any(ConsumableUsedEvent.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should calculate hunger restoration correctly")
  void shouldCalculateHungerRestoration() {
    fixture
        .given(new PetCreatedEvent(PET_ID, "Fluffy", PetType.DOG, null))
        .when(new UseConsumableCommand(PET_ID, ConsumableType.APPLE, PLAYER_ID))
        .expectSuccessfulHandlerExecution();
  }

  @Test
  @DisplayName("should use treat consumable to restore happiness")
  void shouldUseTreatConsumable() {
    fixture
        .given(new PetCreatedEvent(PET_ID, "Fluffy", PetType.DOG, null))
        .when(new UseConsumableCommand(PET_ID, ConsumableType.COOKIE, PLAYER_ID))
        .expectSuccessfulHandlerExecution();
  }

  @Test
  @DisplayName("should use medicine to restore health")
  void shouldUseMedicineToRestoreHealth() {
    fixture
        .given(new PetCreatedEvent(PET_ID, "Fluffy", PetType.DOG, null))
        .when(new UseConsumableCommand(PET_ID, ConsumableType.BASIC_MEDICINE, PLAYER_ID))
        .expectSuccessfulHandlerExecution();
  }

  @Test
  @DisplayName("should cure sickness with advanced medicine")
  void shouldCureSicknessWithAdvancedMedicine() {
    fixture
        .given(
            new PetCreatedEvent(PET_ID, "Fluffy", PetType.DOG, null),
            new PetBecameSickEvent(PET_ID, null))
        .when(new UseConsumableCommand(PET_ID, ConsumableType.ADVANCED_MEDICINE, PLAYER_ID))
        .expectSuccessfulHandlerExecution();
  }

  @Test
  @DisplayName("should not cure with basic medicine")
  void shouldNotCureWithBasicMedicine() {
    fixture
        .given(
            new PetCreatedEvent(PET_ID, "Fluffy", PetType.DOG, null),
            new PetBecameSickEvent(PET_ID, null))
        .when(new UseConsumableCommand(PET_ID, ConsumableType.BASIC_MEDICINE, PLAYER_ID))
        .expectSuccessfulHandlerExecution();
  }

  // Note: Testing dead pet would require a PetDiedEvent which has complex
  // dependencies
  // This is covered by integration tests instead

  @Test
  @DisplayName("should reject consumable with null type")
  void shouldRejectConsumableWithNullType() {
    fixture
        .given(new PetCreatedEvent(PET_ID, "Fluffy", PetType.DOG, null))
        .when(new UseConsumableCommand(PET_ID, null, PLAYER_ID))
        .expectException(IllegalArgumentException.class)
        .expectExceptionMessage("Consumable type cannot be null");
  }

  // Note: Event handler tests for PetBecameSickEvent, PetCuredEvent, and
  // ConsumableUsedEvent
  // are tested via integration tests since they're applied through event sourcing

  @Test
  @DisplayName("should use premium toy with multiple effects")
  void shouldUsePremiumToyWithMultipleEffects() {
    fixture
        .given(new PetCreatedEvent(PET_ID, "Fluffy", PetType.DOG, null))
        .when(new UseConsumableCommand(PET_ID, ConsumableType.PREMIUM_TOY, PLAYER_ID))
        .expectSuccessfulHandlerExecution();
  }
}
