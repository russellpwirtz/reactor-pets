package com.reactor.pets.aggregate;

import static org.axonframework.test.matchers.Matchers.*;
import static org.hamcrest.Matchers.any;

import com.reactor.pets.command.AddConsumableCommand;
import com.reactor.pets.command.RemoveConsumableCommand;
import com.reactor.pets.domain.ConsumableType;
import com.reactor.pets.event.ConsumableAddedEvent;
import com.reactor.pets.event.ConsumableRemovedEvent;
import com.reactor.pets.event.InventoryInitializedEvent;
import java.util.ArrayList;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlayerInventory - Consumables")
class PlayerInventoryConsumableTest {

  private FixtureConfiguration<PlayerInventory> fixture;
  private static final String PLAYER_ID = "PLAYER_INVENTORY";

  @BeforeEach
  void setUp() {
    fixture = new AggregateTestFixture<>(PlayerInventory.class);
  }

  @Test
  @DisplayName("should add consumable to inventory")
  void shouldAddConsumable() {
    fixture
        .given(new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null))
        .when(new AddConsumableCommand(PLAYER_ID, ConsumableType.APPLE, 3))
        .expectSuccessfulHandlerExecution()
        .expectEventsMatching(
            exactSequenceOf(
                messageWithPayload(any(ConsumableAddedEvent.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should track consumable quantity correctly")
  void shouldTrackConsumableQuantity() {
    fixture
        .given(new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null))
        .when(new AddConsumableCommand(PLAYER_ID, ConsumableType.APPLE, 5))
        .expectSuccessfulHandlerExecution();
  }

  @Test
  @DisplayName("should add consumables multiple times and accumulate quantity")
  void shouldAccumulateConsumableQuantity() {
    fixture
        .given(
            new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null),
            new ConsumableAddedEvent(PLAYER_ID, ConsumableType.APPLE, 3, 3, null))
        .when(new AddConsumableCommand(PLAYER_ID, ConsumableType.APPLE, 2))
        .expectSuccessfulHandlerExecution();
  }

  @Test
  @DisplayName("should reject add consumable with null type")
  void shouldRejectAddConsumableWithNullType() {
    fixture
        .given(new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null))
        .when(new AddConsumableCommand(PLAYER_ID, null, 1))
        .expectException(IllegalArgumentException.class)
        .expectExceptionMessage("Consumable type cannot be null");
  }

  @Test
  @DisplayName("should reject add consumable with non-positive quantity")
  void shouldRejectAddConsumableWithNonPositiveQuantity() {
    fixture
        .given(new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null))
        .when(new AddConsumableCommand(PLAYER_ID, ConsumableType.APPLE, 0))
        .expectException(IllegalArgumentException.class)
        .expectExceptionMessage("Quantity must be positive");
  }

  @Test
  @DisplayName("should remove consumable from inventory")
  void shouldRemoveConsumable() {
    fixture
        .given(
            new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null),
            new ConsumableAddedEvent(PLAYER_ID, ConsumableType.APPLE, 5, 5, null))
        .when(new RemoveConsumableCommand(PLAYER_ID, ConsumableType.APPLE, 2))
        .expectSuccessfulHandlerExecution()
        .expectEventsMatching(
            exactSequenceOf(
                messageWithPayload(any(ConsumableRemovedEvent.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should calculate correct remaining quantity after removal")
  void shouldCalculateRemainingQuantity() {
    fixture
        .given(
            new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null),
            new ConsumableAddedEvent(PLAYER_ID, ConsumableType.APPLE, 5, 5, null))
        .when(new RemoveConsumableCommand(PLAYER_ID, ConsumableType.APPLE, 2))
        .expectSuccessfulHandlerExecution();
  }

  @Test
  @DisplayName("should reject remove consumable with insufficient quantity")
  void shouldRejectRemoveConsumableWithInsufficientQuantity() {
    fixture
        .given(
            new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null),
            new ConsumableAddedEvent(PLAYER_ID, ConsumableType.APPLE, 2, 2, null))
        .when(new RemoveConsumableCommand(PLAYER_ID, ConsumableType.APPLE, 5))
        .expectException(IllegalStateException.class);
  }

  @Test
  @DisplayName("should reject remove consumable that doesn't exist")
  void shouldRejectRemoveNonExistentConsumable() {
    fixture
        .given(new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null))
        .when(new RemoveConsumableCommand(PLAYER_ID, ConsumableType.APPLE, 1))
        .expectException(IllegalStateException.class);
  }

  @Test
  @DisplayName("should reject remove consumable with null type")
  void shouldRejectRemoveConsumableWithNullType() {
    fixture
        .given(new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null))
        .when(new RemoveConsumableCommand(PLAYER_ID, null, 1))
        .expectException(IllegalArgumentException.class)
        .expectExceptionMessage("Consumable type cannot be null");
  }

  @Test
  @DisplayName("should reject remove consumable with non-positive quantity")
  void shouldRejectRemoveConsumableWithNonPositiveQuantity() {
    fixture
        .given(
            new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null),
            new ConsumableAddedEvent(PLAYER_ID, ConsumableType.APPLE, 5, 5, null))
        .when(new RemoveConsumableCommand(PLAYER_ID, ConsumableType.APPLE, -1))
        .expectException(IllegalArgumentException.class)
        .expectExceptionMessage("Quantity must be positive");
  }

  @Test
  @DisplayName("should handle multiple different consumable types")
  void shouldHandleMultipleConsumableTypes() {
    fixture
        .given(
            new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null),
            new ConsumableAddedEvent(PLAYER_ID, ConsumableType.APPLE, 3, 3, null),
            new ConsumableAddedEvent(PLAYER_ID, ConsumableType.PIZZA, 2, 2, null))
        .when(new AddConsumableCommand(PLAYER_ID, ConsumableType.COOKIE, 5))
        .expectSuccessfulHandlerExecution();
  }

  @Test
  @DisplayName("should allow removing all of a consumable type")
  void shouldAllowRemovingAllConsumables() {
    fixture
        .given(
            new InventoryInitializedEvent(PLAYER_ID, new ArrayList<>(), null),
            new ConsumableAddedEvent(PLAYER_ID, ConsumableType.APPLE, 3, 3, null))
        .when(new RemoveConsumableCommand(PLAYER_ID, ConsumableType.APPLE, 3))
        .expectSuccessfulHandlerExecution();
  }
}
