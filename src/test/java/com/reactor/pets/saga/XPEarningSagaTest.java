package com.reactor.pets.saga;

import static org.axonframework.test.matchers.Matchers.andNoMore;
import static org.axonframework.test.matchers.Matchers.exactSequenceOf;
import static org.axonframework.test.matchers.Matchers.messageWithPayload;
import static org.hamcrest.Matchers.any;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.command.EarnXPCommand;
import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetEvolvedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import com.reactor.pets.event.TimePassedEvent;
import java.time.Instant;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for XPEarningSaga. */
@DisplayName("XPEarningSaga")
class XPEarningSagaTest {

  private SagaTestFixture<XPEarningSaga> fixture;
  private static final String PET_ID = "test-pet-123";
  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    fixture = new SagaTestFixture<>(XPEarningSaga.class);
  }

  @Test
  @DisplayName("should start saga on PetCreatedEvent and initialize XP multiplier")
  void shouldStartSagaOnPetCreatedEvent() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands(); // No commands - just initializes state
  }

  @Test
  @DisplayName("should earn XP when pet is fed")
  void shouldEarnXPWhenPetIsFed() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .whenPublishingA(new PetFedEvent(PET_ID, 15, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EarnXPCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should earn XP when playing with pet")
  void shouldEarnXPWhenPlayingWithPet() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .whenPublishingA(new PetPlayedWithEvent(PET_ID, 10, 5, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EarnXPCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should earn XP when pet is cleaned")
  void shouldEarnXPWhenPetIsCleaned() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .whenPublishingA(new PetCleanedEvent(PET_ID, 10, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EarnXPCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should earn survival XP on TimePassedEvent")
  void shouldEarnSurvivalXPOnTimePassedEvent() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .whenPublishingA(new TimePassedEvent(PET_ID, 3, 2, 1, 1L, 0.0, 1.0, 0, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EarnXPCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should earn bonus XP when pet evolves to BABY")
  void shouldEarnBonusXPWhenEvolvingToBaby() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .whenPublishingA(
            new PetEvolvedEvent(
                PET_ID, PetStage.EGG, PetStage.BABY, EvolutionPath.HEALTHY, "Hatched", NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EarnXPCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should earn bonus XP when pet evolves to TEEN")
  void shouldEarnBonusXPWhenEvolvingToTeen() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .whenPublishingA(
            new PetEvolvedEvent(
                PET_ID, PetStage.BABY, PetStage.TEEN, EvolutionPath.HEALTHY, "Grew up", NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EarnXPCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should earn bonus XP when pet evolves to ADULT")
  void shouldEarnBonusXPWhenEvolvingToAdult() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .whenPublishingA(
            new PetEvolvedEvent(
                PET_ID, PetStage.TEEN, PetStage.ADULT, EvolutionPath.HEALTHY, "Matured", NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EarnXPCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should track XP multiplier from TimePassedEvent")
  void shouldTrackXPMultiplierFromTimePassedEvent() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        // First time tick with 1.5x multiplier
        .andThenAPublished(new TimePassedEvent(PET_ID, 3, 2, 1, 1L, 0.0, 1.5, 0, NOW))
        // Feed with the new multiplier
        .whenPublishingA(new PetFedEvent(PET_ID, 15, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EarnXPCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should handle multiple actions earning XP")
  void shouldHandleMultipleActionsEarningXP() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .andThenAPublished(new PetFedEvent(PET_ID, 15, NOW))
        .andThenAPublished(new PetPlayedWithEvent(PET_ID, 10, 5, NOW))
        .andThenAPublished(new PetCleanedEvent(PET_ID, 10, NOW))
        .whenPublishingA(new TimePassedEvent(PET_ID, 3, 2, 1, 1L, 0.0, 1.0, 0, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EarnXPCommand.class)), andNoMore()));
  }
}
