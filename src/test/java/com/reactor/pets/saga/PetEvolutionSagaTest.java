package com.reactor.pets.saga;

import static org.axonframework.test.matchers.Matchers.andNoMore;
import static org.axonframework.test.matchers.Matchers.exactSequenceOf;
import static org.axonframework.test.matchers.Matchers.messageWithPayload;
import static org.hamcrest.Matchers.any;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.command.EvolvePetCommand;
import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.event.PetEvolvedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetHealthDeterioratedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import com.reactor.pets.event.TimePassedEvent;
import java.time.Instant;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for PetEvolutionSaga. */
@DisplayName("PetEvolutionSaga")
class PetEvolutionSagaTest {

  private SagaTestFixture<PetEvolutionSaga> fixture;
  private static final String PET_ID = "test-pet-123";
  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    fixture = new SagaTestFixture<>(PetEvolutionSaga.class);
  }

  @Test
  @DisplayName("should start saga on PetCreatedEvent")
  void shouldStartSagaOnPetCreatedEvent() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands();
  }

  @Test
  @DisplayName("should track age on TimePassedEvent")
  void shouldTrackAgeOnTimePassedEvent() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .whenPublishingA(new TimePassedEvent(PET_ID, 3, 2, 1, 1L, NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands(); // Age 1 is not enough to evolve from EGG
  }

  @Test
  @DisplayName("should evolve from EGG to BABY at age 5")
  void shouldEvolveFromEggToBabyAtAge5() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        // Age pet to 5
        .whenPublishingA(new TimePassedEvent(PET_ID, 15, 10, 5, 5L, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EvolvePetCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should evolve from BABY to TEEN at age 20")
  void shouldEvolveFromBabyToTeenAtAge20() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        // Evolve to BABY first
        .andThenAPublished(new TimePassedEvent(PET_ID, 15, 10, 5, 5L, NOW))
        .andThenAPublished(
            new PetEvolvedEvent(
                PET_ID, PetStage.EGG, PetStage.BABY, EvolutionPath.HEALTHY, "Hatched", NOW))
        // Age to 20 (15 more ticks)
        .whenPublishingA(new TimePassedEvent(PET_ID, 45, 30, 15, 20L, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EvolvePetCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should evolve from TEEN to ADULT at age 50 with good care")
  void shouldEvolveFromTeenToAdultAtAge50WithGoodCare() {
    // Setup: Create pet, evolve to BABY, then TEEN
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 15, 10, 5, 5L, NOW))
        .andThenAPublished(
            new PetEvolvedEvent(
                PET_ID, PetStage.EGG, PetStage.BABY, EvolutionPath.HEALTHY, "Hatched", NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 45, 30, 15, 20L, NOW))
        .andThenAPublished(
            new PetEvolvedEvent(
                PET_ID, PetStage.BABY, PetStage.TEEN, EvolutionPath.HEALTHY, "Grew up", NOW))
        // Maintain good health by cleaning regularly
        .andThenAPublished(new PetCleanedEvent(PET_ID, 10, NOW))
        .andThenAPublished(new PetCleanedEvent(PET_ID, 10, NOW))
        // Age to 50 (30 more, with time ticks to build history)
        .andThenAPublished(new TimePassedEvent(PET_ID, 30, 20, 10, 30L, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 30, 20, 10, 40L, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 30, 20, 10, 50L, NOW))
        .whenPublishingA(new TimePassedEvent(PET_ID, 0, 0, 0, 50L, NOW)) // Trigger check at age 50
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EvolvePetCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should update stats on PetFedEvent")
  void shouldUpdateStatsOnPetFedEvent() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .whenPublishingA(new PetFedEvent(PET_ID, 15, NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands();
  }

  @Test
  @DisplayName("should update stats on PetPlayedWithEvent")
  void shouldUpdateStatsOnPetPlayedWithEvent() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .whenPublishingA(new PetPlayedWithEvent(PET_ID, 10, 5, NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands();
  }

  @Test
  @DisplayName("should update stats on PetCleanedEvent")
  void shouldUpdateStatsOnPetCleanedEvent() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .whenPublishingA(new PetCleanedEvent(PET_ID, 10, NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands();
  }

  @Test
  @DisplayName("should update stats on PetHealthDeterioratedEvent")
  void shouldUpdateStatsOnPetHealthDeterioratedEvent() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .whenPublishingA(new PetHealthDeterioratedEvent(PET_ID, 20, "Starvation", NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands();
  }

  @Test
  @DisplayName("should update stage on PetEvolvedEvent")
  void shouldUpdateStageOnPetEvolvedEvent() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .whenPublishingA(
            new PetEvolvedEvent(
                PET_ID, PetStage.EGG, PetStage.BABY, EvolutionPath.HEALTHY, "Hatched", NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands();
  }

  @Test
  @DisplayName("should end saga on PetDiedEvent")
  void shouldEndSagaOnPetDiedEvent() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .whenPublishingA(new PetDiedEvent(PET_ID, 10, 100, "Neglect", NOW))
        .expectActiveSagas(0); // Saga should end
  }

  @Test
  @DisplayName("should not evolve at ADULT stage")
  void shouldNotEvolveAtAdultStage() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .andThenAPublished(
            new PetEvolvedEvent(
                PET_ID, PetStage.EGG, PetStage.BABY, EvolutionPath.HEALTHY, "Hatched", NOW))
        .andThenAPublished(
            new PetEvolvedEvent(
                PET_ID, PetStage.BABY, PetStage.TEEN, EvolutionPath.HEALTHY, "Grew up", NOW))
        .andThenAPublished(
            new PetEvolvedEvent(
                PET_ID, PetStage.TEEN, PetStage.ADULT, EvolutionPath.HEALTHY, "Matured", NOW))
        // Try to age past adult stage
        .whenPublishingA(new TimePassedEvent(PET_ID, 300, 200, 100, 150L, NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands(); // No further evolution
  }

  @Test
  @DisplayName("should determine HEALTHY evolution path with good care")
  void shouldDetermineHealthyEvolutionPathWithGoodCare() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        // Keep pet healthy and happy
        .andThenAPublished(new PetCleanedEvent(PET_ID, 20, NOW)) // Health boost
        .andThenAPublished(new PetPlayedWithEvent(PET_ID, 20, 5, NOW)) // Happiness boost
        .andThenAPublished(new TimePassedEvent(PET_ID, 3, 2, 1, 1L, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 3, 2, 1, 2L, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 3, 2, 1, 3L, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 3, 2, 1, 4L, NOW))
        // Age to 5 to trigger evolution
        .whenPublishingA(new TimePassedEvent(PET_ID, 3, 2, 1, 5L, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EvolvePetCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should determine NEGLECTED evolution path with poor care")
  void shouldDetermineNeglectedEvolutionPathWithPoorCare() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        // Neglect the pet - let health and happiness degrade
        .andThenAPublished(new PetHealthDeterioratedEvent(PET_ID, 40, "Hunger", NOW)) // Health to 60
        .andThenAPublished(new TimePassedEvent(PET_ID, 3, 5, 1, 1L, NOW)) // Happiness drops
        .andThenAPublished(
            new PetHealthDeterioratedEvent(PET_ID, 30, "Starvation", NOW)) // Health to 30
        .andThenAPublished(new TimePassedEvent(PET_ID, 3, 5, 1, 2L, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 3, 5, 1, 3L, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 3, 5, 1, 4L, NOW))
        // Age to 5 to trigger evolution
        .whenPublishingA(new TimePassedEvent(PET_ID, 3, 5, 1, 5L, NOW))
        .expectActiveSagas(1)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(messageWithPayload(any(EvolvePetCommand.class)), andNoMore()));
  }

  @Test
  @DisplayName("should not evolve BABY to TEEN before age 20")
  void shouldNotEvolveBabyToTeenBeforeAge20() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 15, 10, 5, 5L, NOW))
        .andThenAPublished(
            new PetEvolvedEvent(
                PET_ID, PetStage.EGG, PetStage.BABY, EvolutionPath.HEALTHY, "Hatched", NOW))
        // Age to 19 (not quite 20 yet)
        .whenPublishingA(new TimePassedEvent(PET_ID, 42, 28, 14, 19L, NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands(); // Should not evolve yet
  }

  @Test
  @DisplayName("should not evolve TEEN to ADULT before age 50")
  void shouldNotEvolveTeenToAdultBeforeAge50() {
    fixture
        .givenAPublished(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 15, 10, 5, 5L, NOW))
        .andThenAPublished(
            new PetEvolvedEvent(
                PET_ID, PetStage.EGG, PetStage.BABY, EvolutionPath.HEALTHY, "Hatched", NOW))
        .andThenAPublished(new TimePassedEvent(PET_ID, 45, 30, 15, 20L, NOW))
        .andThenAPublished(
            new PetEvolvedEvent(
                PET_ID, PetStage.BABY, PetStage.TEEN, EvolutionPath.HEALTHY, "Grew up", NOW))
        // Age to 49 (not quite 50 yet)
        .whenPublishingA(new TimePassedEvent(PET_ID, 87, 58, 29, 49L, NOW))
        .expectActiveSagas(1)
        .expectNoDispatchedCommands(); // Should not evolve yet
  }
}
