package com.reactor.pets.saga;

import static org.axonframework.test.matchers.Matchers.andNoMore;
import static org.axonframework.test.matchers.Matchers.exactSequenceOf;
import static org.axonframework.test.matchers.Matchers.messageWithPayload;
import static org.hamcrest.Matchers.any;

import com.reactor.pets.command.AddItemToInventoryCommand;
import com.reactor.pets.command.PurchaseEquipmentCommand;
import com.reactor.pets.command.PurchaseUpgradeCommand;
import com.reactor.pets.command.SpendXPCommand;
import com.reactor.pets.domain.UpgradeType;
import com.reactor.pets.event.XPSpentEvent;
import java.time.Instant;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ShopPurchaseSaga.
 */
@DisplayName("ShopPurchaseSaga")
class ShopPurchaseSagaTest {

  private SagaTestFixture<ShopPurchaseSaga> fixture;
  private static final String PLAYER_ID = "PLAYER";
  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    fixture = new SagaTestFixture<>(ShopPurchaseSaga.class);
  }

  @Test
  @DisplayName("should start saga on PurchaseEquipmentCommand and spend XP")
  void shouldStartSagaOnPurchaseEquipmentCommand() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseEquipmentCommand(PLAYER_ID, "SLOW_FEEDER"))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should add item to inventory after XP spent for equipment")
  void shouldAddItemToInventoryAfterXPSpent() {
    fixture
        .givenAPublished(new PurchaseEquipmentCommand(PLAYER_ID, "SLOW_FEEDER"))
        .whenPublishingA(new XPSpentEvent(
            PLAYER_ID,
            200L,
            0L,
            "Purchase: Slow Feeder",
            NOW))
        .expectActiveSagas(0) // Saga should end after successful purchase
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(AddItemToInventoryCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should end saga on correct XP spent event")
  void shouldEndSagaOnCorrectXPSpentEvent() {
    fixture
        .givenAPublished(new PurchaseEquipmentCommand(PLAYER_ID, "SLOW_FEEDER"))
        .whenPublishingA(new XPSpentEvent(
            PLAYER_ID,
            200L,
            0L,
            "Purchase: Slow Feeder", // Correct purpose
            NOW))
        .expectActiveSagas(0) // Saga should end
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(AddItemToInventoryCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should handle upgrade purchase command")
  void shouldHandleUpgradePurchaseCommand() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseUpgradeCommand(
            PLAYER_ID,
            UpgradeType.EFFICIENT_METABOLISM))
        .expectActiveSagas(0) // Upgrade saga ends immediately
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase nutrient bowl equipment")
  void shouldPurchaseNutrientBowl() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseEquipmentCommand(PLAYER_ID, "NUTRIENT_BOWL"))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should complete nutrient bowl purchase after XP spent")
  void shouldCompleteNutrientBowlPurchase() {
    fixture
        .givenAPublished(new PurchaseEquipmentCommand(PLAYER_ID, "NUTRIENT_BOWL"))
        .whenPublishingA(new XPSpentEvent(
            PLAYER_ID,
            300L,
            0L,
            "Purchase: Nutrient Bowl",
            NOW))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(AddItemToInventoryCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase auto feeder equipment")
  void shouldPurchaseAutoFeeder() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseEquipmentCommand(PLAYER_ID, "AUTO_FEEDER"))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase toy box equipment")
  void shouldPurchaseToyBox() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseEquipmentCommand(PLAYER_ID, "TOY_BOX"))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase exercise wheel equipment")
  void shouldPurchaseExerciseWheel() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseEquipmentCommand(PLAYER_ID, "EXERCISE_WHEEL"))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase entertainment system equipment")
  void shouldPurchaseEntertainmentSystem() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseEquipmentCommand(PLAYER_ID, "ENTERTAINMENT_SYSTEM"))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase cozy bed accessory")
  void shouldPurchaseCozyBed() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseEquipmentCommand(PLAYER_ID, "COZY_BED"))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase health monitor accessory")
  void shouldPurchaseHealthMonitor() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseEquipmentCommand(PLAYER_ID, "HEALTH_MONITOR"))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase XP charm accessory")
  void shouldPurchaseXPCharm() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseEquipmentCommand(PLAYER_ID, "XP_CHARM"))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase happy disposition upgrade")
  void shouldPurchaseHappyDisposition() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseUpgradeCommand(
            PLAYER_ID,
            UpgradeType.HAPPY_DISPOSITION))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase sturdy genetics upgrade")
  void shouldPurchaseSturdyGenetics() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseUpgradeCommand(
            PLAYER_ID,
            UpgradeType.STURDY_GENETICS))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase industrial kitchen upgrade")
  void shouldPurchaseIndustrialKitchen() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseUpgradeCommand(
            PLAYER_ID,
            UpgradeType.INDUSTRIAL_KITCHEN))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase fast hatcher upgrade")
  void shouldPurchaseFastHatcher() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseUpgradeCommand(
            PLAYER_ID,
            UpgradeType.FAST_HATCHER))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase multi-pet license I")
  void shouldPurchaseMultiPetLicenseI() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseUpgradeCommand(
            PLAYER_ID,
            UpgradeType.MULTI_PET_LICENSE_I))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase multi-pet license II")
  void shouldPurchaseMultiPetLicenseII() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseUpgradeCommand(
            PLAYER_ID,
            UpgradeType.MULTI_PET_LICENSE_II))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should purchase multi-pet license III")
  void shouldPurchaseMultiPetLicenseIII() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PurchaseUpgradeCommand(
            PLAYER_ID,
            UpgradeType.MULTI_PET_LICENSE_III))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(SpendXPCommand.class)),
                andNoMore()));
  }
}
