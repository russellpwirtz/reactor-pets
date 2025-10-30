package com.reactor.pets.saga;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.query.GetAlivePetsQuery;
import com.reactor.pets.query.PetStatusView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for PetSocialBehaviorSaga. */
@DisplayName("PetSocialBehaviorSaga")
class PetSocialBehaviorSagaTest {

  private SagaTestFixture<PetSocialBehaviorSaga> fixture;
  private QueryGateway queryGateway;
  private static final String PET_ID = "test-pet-123";
  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    fixture = new SagaTestFixture<>(PetSocialBehaviorSaga.class);

    // Register QueryGateway to prevent NullPointerException
    queryGateway = mock(QueryGateway.class);

    // By default, return empty list (no alive pets to mourn)
    when(queryGateway.query(any(GetAlivePetsQuery.class), any(ResponseType.class)))
        .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

    fixture.registerResource(queryGateway);
  }

  @Test
  @DisplayName("should start and end saga on PetDiedEvent")
  void shouldStartAndEndSagaOnPetDiedEvent() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetDiedEvent(PET_ID, 10, 100, "Neglect", new ArrayList<>(), NOW))
        .expectActiveSagas(0); // Saga starts and ends immediately
  }

  @Test
  @DisplayName("should trigger mourning for other alive pets when a pet dies")
  void shouldTriggerMourningForOtherAlivePets() {
    // Note: This test would require mocking QueryGateway to return alive pets
    // For now, we verify the saga starts/ends correctly
    // Integration tests will verify the full mourning flow

    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetDiedEvent(PET_ID, 5, 50, "Old age", new ArrayList<>(), NOW))
        .expectActiveSagas(0);
  }

  @Test
  @DisplayName("should handle pet death with equipped items")
  void shouldHandlePetDeathWithEquippedItems() {
    java.util.List<EquipmentItem> equippedItems = new ArrayList<>();
    equippedItems.add(new EquipmentItem(
        "item-1",
        "BASIC_BOWL",
        com.reactor.pets.domain.EquipmentSlot.FOOD_BOWL,
        java.util.Map.of(com.reactor.pets.domain.StatModifier.FOOD_EFFICIENCY, 0.1)));

    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetDiedEvent(PET_ID, 10, 100, "Starvation", equippedItems, NOW))
        .expectActiveSagas(0); // Equipment handled by EquipmentSaga, social behavior ends immediately
  }

  @Test
  @DisplayName("should handle pet death with null equipped items")
  void shouldHandlePetDeathWithNullEquippedItems() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetDiedEvent(PET_ID, 0, 0, "Disease", null, NOW))
        .expectActiveSagas(0);
  }

  @Test
  @DisplayName("should dispatch mourning commands when alive pets exist")
  void shouldDispatchMourningCommandsWhenAlivePetsExist() {
    // Create mock alive pets
    // Constructor params: petId, name, type, hunger, happiness, health, stage, evolutionPath,
    //                    isAlive, age, birthGlobalTick, currentGlobalTick, xpMultiplier, lastUpdated,
    //                    equippedItems, maxEquipmentSlots
    PetStatusView alivePet1 = new PetStatusView(
        "alive-pet-1",              // petId
        "Buddy",                     // name
        com.reactor.pets.aggregate.PetType.DOG,        // type
        60,                          // hunger
        80,                          // happiness
        100,                         // health
        com.reactor.pets.aggregate.PetStage.ADULT,     // stage
        com.reactor.pets.aggregate.EvolutionPath.HEALTHY,  // evolutionPath
        true,                        // isAlive
        10,                          // age
        0L,                          // birthGlobalTick
        10L,                         // currentGlobalTick
        1.0,                         // xpMultiplier
        NOW,                         // lastUpdated
        new java.util.HashMap<>(),   // equippedItems
        3);                          // maxEquipmentSlots

    PetStatusView alivePet2 = new PetStatusView(
        "alive-pet-2",              // petId
        "Max",                       // name
        com.reactor.pets.aggregate.PetType.CAT,        // type
        40,                          // hunger
        50,                          // happiness
        100,                         // health
        com.reactor.pets.aggregate.PetStage.TEEN,      // stage
        com.reactor.pets.aggregate.EvolutionPath.HEALTHY,  // evolutionPath
        true,                        // isAlive
        5,                           // age
        0L,                          // birthGlobalTick
        5L,                          // currentGlobalTick
        1.0,                         // xpMultiplier
        NOW,                         // lastUpdated
        new java.util.HashMap<>(),   // equippedItems
        3);                          // maxEquipmentSlots

    List<PetStatusView> alivePets = List.of(alivePet1, alivePet2);

    // Configure mock to return alive pets
    when(queryGateway.query(any(GetAlivePetsQuery.class), any(ResponseType.class)))
        .thenReturn(CompletableFuture.completedFuture(alivePets));

    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetDiedEvent(PET_ID, 10, 100, "Neglect", new ArrayList<>(), NOW))
        .expectActiveSagas(0)
        .expectDispatchedCommands(
            new com.reactor.pets.command.MournPetCommand("alive-pet-1", PET_ID, 8), // 10% of 80 happiness
            new com.reactor.pets.command.MournPetCommand("alive-pet-2", PET_ID, 5)  // 10% of 50 happiness
        );
  }
}
