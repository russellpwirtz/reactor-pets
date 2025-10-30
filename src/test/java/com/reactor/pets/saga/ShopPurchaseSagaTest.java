package com.reactor.pets.saga;

import static org.axonframework.test.matchers.Matchers.andNoMore;
import static org.axonframework.test.matchers.Matchers.exactSequenceOf;
import static org.axonframework.test.matchers.Matchers.messageWithPayload;
import static org.hamcrest.Matchers.any;

import com.reactor.pets.command.AddItemToInventoryCommand;
import com.reactor.pets.event.EquipmentPurchasedEvent;
import java.time.Instant;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ShopPurchaseSaga.
 * Tests the event-driven flow where the saga listens to EquipmentPurchasedEvent
 * (emitted by PlayerProgression aggregate) and adds items to inventory.
 */
@DisplayName("ShopPurchaseSaga")
class ShopPurchaseSagaTest {

    private SagaTestFixture<ShopPurchaseSaga> fixture;
    private static final String PLAYER_ID = "PLAYER_1";
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(ShopPurchaseSaga.class);
    }

    @Test
    @DisplayName("should start saga on EquipmentPurchasedEvent and add to inventory")
    void shouldStartSagaOnEquipmentPurchasedEvent() {
        fixture
                .givenNoPriorActivity()
                .whenPublishingA(new EquipmentPurchasedEvent(
                        PLAYER_ID,
                        "SLOW_FEEDER",
                        200L,
                        "Slow Feeder",
                        NOW))
                .expectActiveSagas(0) // Saga starts and ends immediately
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                messageWithPayload(any(AddItemToInventoryCommand.class)),
                                andNoMore()));
    }

    @Test
    @DisplayName("should add nutrient bowl to inventory on purchase event")
    void shouldPurchaseNutrientBowl() {
        fixture
                .givenNoPriorActivity()
                .whenPublishingA(new EquipmentPurchasedEvent(
                        PLAYER_ID,
                        "NUTRIENT_BOWL",
                        300L,
                        "Nutrient Bowl",
                        NOW))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                messageWithPayload(any(AddItemToInventoryCommand.class)),
                                andNoMore()));
    }

    @Test
    @DisplayName("should add auto feeder to inventory on purchase event")
    void shouldPurchaseAutoFeeder() {
        fixture
                .givenNoPriorActivity()
                .whenPublishingA(new EquipmentPurchasedEvent(
                        PLAYER_ID,
                        "AUTO_FEEDER",
                        500L,
                        "Auto-Feeder",
                        NOW))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                messageWithPayload(any(AddItemToInventoryCommand.class)),
                                andNoMore()));
    }

    @Test
    @DisplayName("should add toy box to inventory on purchase event")
    void shouldPurchaseToyBox() {
        fixture
                .givenNoPriorActivity()
                .whenPublishingA(new EquipmentPurchasedEvent(
                        PLAYER_ID,
                        "TOY_BOX",
                        200L,
                        "Toy Box",
                        NOW))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                messageWithPayload(any(AddItemToInventoryCommand.class)),
                                andNoMore()));
    }

    @Test
    @DisplayName("should add exercise wheel to inventory on purchase event")
    void shouldPurchaseExerciseWheel() {
        fixture
                .givenNoPriorActivity()
                .whenPublishingA(new EquipmentPurchasedEvent(
                        PLAYER_ID,
                        "EXERCISE_WHEEL",
                        300L,
                        "Exercise Wheel",
                        NOW))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                messageWithPayload(any(AddItemToInventoryCommand.class)),
                                andNoMore()));
    }

    @Test
    @DisplayName("should add entertainment system to inventory on purchase event")
    void shouldPurchaseEntertainmentSystem() {
        fixture
                .givenNoPriorActivity()
                .whenPublishingA(new EquipmentPurchasedEvent(
                        PLAYER_ID,
                        "ENTERTAINMENT_SYSTEM",
                        500L,
                        "Entertainment System",
                        NOW))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                messageWithPayload(any(AddItemToInventoryCommand.class)),
                                andNoMore()));
    }

    @Test
    @DisplayName("should add cozy bed to inventory on purchase event")
    void shouldPurchaseCozyBed() {
        fixture
                .givenNoPriorActivity()
                .whenPublishingA(new EquipmentPurchasedEvent(
                        PLAYER_ID,
                        "COZY_BED",
                        200L,
                        "Cozy Bed",
                        NOW))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                messageWithPayload(any(AddItemToInventoryCommand.class)),
                                andNoMore()));
    }

    @Test
    @DisplayName("should add health monitor to inventory on purchase event")
    void shouldPurchaseHealthMonitor() {
        fixture
                .givenNoPriorActivity()
                .whenPublishingA(new EquipmentPurchasedEvent(
                        PLAYER_ID,
                        "HEALTH_MONITOR",
                        400L,
                        "Health Monitor",
                        NOW))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                messageWithPayload(any(AddItemToInventoryCommand.class)),
                                andNoMore()));
    }

    @Test
    @DisplayName("should add XP charm to inventory on purchase event")
    void shouldPurchaseXPCharm() {
        fixture
                .givenNoPriorActivity()
                .whenPublishingA(new EquipmentPurchasedEvent(
                        PLAYER_ID,
                        "XP_CHARM",
                        600L,
                        "XP Charm",
                        NOW))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                messageWithPayload(any(AddItemToInventoryCommand.class)),
                                andNoMore()));
    }
}
