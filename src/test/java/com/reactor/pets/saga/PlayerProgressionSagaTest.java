package com.reactor.pets.saga;

import static org.axonframework.test.matchers.Matchers.andNoMore;
import static org.axonframework.test.matchers.Matchers.exactSequenceOf;
import static org.axonframework.test.matchers.Matchers.messageWithPayload;
import static org.hamcrest.Matchers.any;

import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.command.TrackPetCreationCommand;
import com.reactor.pets.event.PetCreatedEvent;
import java.time.Instant;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for PlayerProgressionSaga. */
@DisplayName("PlayerProgressionSaga")
class PlayerProgressionSagaTest {

  private SagaTestFixture<PlayerProgressionSaga> fixture;
  private static final String PET_ID = "test-pet-123";
  private static final Instant NOW = Instant.now();

  @BeforeEach
  void setUp() {
    fixture = new SagaTestFixture<>(PlayerProgressionSaga.class);
  }

  @Test
  @DisplayName("should start and end saga on PetCreatedEvent and dispatch TrackPetCreationCommand")
  void shouldDispatchTrackPetCreationCommand() {
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetCreatedEvent(PET_ID, "Test Pet", PetType.DOG, 0L, NOW))
        .expectActiveSagas(0) // Saga ends immediately (@StartSaga + @EndSaga)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(TrackPetCreationCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should dispatch TrackPetCreationCommand with correct pet details")
  void shouldIncludeCorrectPetDetails() {
    String petName = "Fluffy";
    PetType petType = PetType.CAT;

    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetCreatedEvent(PET_ID, petName, petType, 0L, NOW))
        .expectActiveSagas(0)
        .expectDispatchedCommands(
            new TrackPetCreationCommand(
                "PLAYER_1",
                PET_ID,
                petName,
                petType));
  }

  @Test
  @DisplayName("should handle multiple pet creations independently")
  void shouldHandleMultiplePetCreations() {
    String petId1 = "pet-1";
    String petId2 = "pet-2";

    // First pet creation
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetCreatedEvent(petId1, "Pet 1", PetType.DOG, 0L, NOW))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(TrackPetCreationCommand.class)),
                andNoMore()));

    // Second pet creation (separate saga instance)
    fixture
        .givenNoPriorActivity()
        .whenPublishingA(new PetCreatedEvent(petId2, "Pet 2", PetType.CAT, 0L, NOW))
        .expectActiveSagas(0)
        .expectDispatchedCommandsMatching(
            exactSequenceOf(
                messageWithPayload(any(TrackPetCreationCommand.class)),
                andNoMore()));
  }

  @Test
  @DisplayName("should handle all pet types correctly")
  void shouldHandleAllPetTypes() {
    for (PetType type : PetType.values()) {
      String petId = "pet-" + type.name().toLowerCase();

      fixture
          .givenNoPriorActivity()
          .whenPublishingA(new PetCreatedEvent(petId, "Test " + type, type, 0L, NOW))
          .expectActiveSagas(0)
          .expectDispatchedCommands(
              new TrackPetCreationCommand("PLAYER_1", petId, "Test " + type, type));
    }
  }
}
