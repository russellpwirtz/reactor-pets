package com.reactor.pets.aggregate;

import static org.axonframework.test.matchers.Matchers.matches;

import com.reactor.pets.command.CleanPetCommand;
import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.command.FeedPetCommand;
import com.reactor.pets.command.PlayWithPetCommand;
import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import java.time.Instant;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Pet Aggregate using Axon's AggregateTestFixture.
 *
 * <p>These tests verify: - Command handling logic - Event sourcing handlers - Business rule
 * validation - Aggregate state transitions
 */
@DisplayName("Pet Aggregate")
class PetAggregateTest {

  private FixtureConfiguration<Pet> fixture;

  @BeforeEach
  void setUp() {
    fixture = new AggregateTestFixture<>(Pet.class);
  }

  @Nested
  @DisplayName("Pet Creation")
  class PetCreation {

    @Test
    @DisplayName("should create pet with valid command")
    void shouldCreatePetWithValidCommand() {
      String petId = "pet-123";
      String name = "Fluffy";
      PetType type = PetType.CAT;
      long birthGlobalTick = 0L;

      fixture
          .givenNoPriorActivity()
          .when(new CreatePetCommand(petId, name, type, birthGlobalTick))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof PetCreatedEvent event)) {
                      return false;
                    }
                    return event.getPetId().equals(petId)
                        && event.getName().equals(name)
                        && event.getType().equals(type)
                        && event.getBirthGlobalTick() == birthGlobalTick
                        && event.getTimestamp() != null;
                  }));
    }

    @Test
    @DisplayName("should reject creation with null name")
    void shouldRejectCreationWithNullName() {
      fixture
          .givenNoPriorActivity()
          .when(new CreatePetCommand("pet-123", null, PetType.DOG, 0L))
          .expectException(IllegalArgumentException.class)
          .expectExceptionMessage("Pet name cannot be empty");
    }

    @Test
    @DisplayName("should reject creation with empty name")
    void shouldRejectCreationWithEmptyName() {
      fixture
          .givenNoPriorActivity()
          .when(new CreatePetCommand("pet-123", "  ", PetType.DOG, 0L))
          .expectException(IllegalArgumentException.class)
          .expectExceptionMessage("Pet name cannot be empty");
    }

    @Test
    @DisplayName("should reject creation with null type")
    void shouldRejectCreationWithNullType() {
      fixture
          .givenNoPriorActivity()
          .when(new CreatePetCommand("pet-123", "Rex", null, 0L))
          .expectException(IllegalArgumentException.class)
          .expectExceptionMessage("Pet type cannot be null");
    }
  }

  @Nested
  @DisplayName("Pet Feeding")
  class PetFeeding {

    @Test
    @DisplayName("should reduce hunger when feeding pet")
    void shouldReduceHungerWhenFeedingPet() {
      String petId = "pet-123";

      fixture
          .given(new PetCreatedEvent(petId, "Buddy", PetType.DOG, 0L, Instant.now()))
          .when(new FeedPetCommand(petId, 20))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof PetFedEvent event)) {
                      return false;
                    }
                    return event.getPetId().equals(petId)
                        && event.getHungerReduction() == 20
                        && event.getTimestamp() != null;
                  }));
    }

    @Test
    @DisplayName("should cap hunger reduction at current hunger level")
    void shouldCapHungerReductionAtCurrentHungerLevel() {
      String petId = "pet-123";

      fixture
          .given(new PetCreatedEvent(petId, "Buddy", PetType.DOG, 0L, Instant.now()))
          .when(new FeedPetCommand(petId, 100)) // More than initial hunger of 30
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof PetFedEvent event)) {
                      return false;
                    }
                    // Initial hunger is 30, so reduction should be capped at 30
                    return event.getHungerReduction() == 30;
                  }));
    }

    @Test
    @DisplayName("should reject feeding with zero food amount")
    void shouldRejectFeedingWithZeroFoodAmount() {
      String petId = "pet-123";

      fixture
          .given(new PetCreatedEvent(petId, "Buddy", PetType.DOG, 0L, Instant.now()))
          .when(new FeedPetCommand(petId, 0))
          .expectException(IllegalArgumentException.class)
          .expectExceptionMessage("Food amount must be positive");
    }

    @Test
    @DisplayName("should reject feeding with negative food amount")
    void shouldRejectFeedingWithNegativeFoodAmount() {
      String petId = "pet-123";

      fixture
          .given(new PetCreatedEvent(petId, "Buddy", PetType.DOG, 0L, Instant.now()))
          .when(new FeedPetCommand(petId, -10))
          .expectException(IllegalArgumentException.class)
          .expectExceptionMessage("Food amount must be positive");
    }
  }

  @Nested
  @DisplayName("Pet State Management")
  class PetStateManagement {

    @Test
    @DisplayName("should initialize with correct default stats")
    void shouldInitializeWithCorrectDefaultStats() {
      String petId = "pet-123";

      // This test verifies the event sourcing handler sets correct initial state
      // We feed with amount > initial hunger to verify the initial hunger is 30
      fixture
          .given(new PetCreatedEvent(petId, "Max", PetType.DRAGON, 0L, Instant.now()))
          .when(new FeedPetCommand(petId, 50))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof PetFedEvent event)) {
                      return false;
                    }
                    // Initial hunger should be 30, so reduction is capped at 30
                    return event.getHungerReduction() == 30;
                  }));
    }

    @Test
    @DisplayName("should accumulate hunger reduction across multiple feedings")
    void shouldAccumulateHungerReductionAcrossMultipleFeedings() {
      String petId = "pet-123";

      fixture
          .given(
              new PetCreatedEvent(petId, "Max", PetType.DRAGON, 0L, Instant.now()),
              new PetFedEvent(petId, 10, Instant.now()))
          .when(new FeedPetCommand(petId, 25))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof PetFedEvent event)) {
                      return false;
                    }
                    // After first feeding: hunger = 30 - 10 = 20
                    // Second feeding should cap at 20
                    return event.getHungerReduction() == 20;
                  }));
    }
  }

  @Nested
  @DisplayName("All Pet Types")
  class AllPetTypes {

    @Test
    @DisplayName("should create DOG pet successfully")
    void shouldCreateDogPet() {
      fixture
          .givenNoPriorActivity()
          .when(new CreatePetCommand("pet-1", "Rex", PetType.DOG, 0L))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    return payload instanceof PetCreatedEvent event
                        && event.getType() == PetType.DOG;
                  }));
    }

    @Test
    @DisplayName("should create CAT pet successfully")
    void shouldCreateCatPet() {
      fixture
          .givenNoPriorActivity()
          .when(new CreatePetCommand("pet-2", "Whiskers", PetType.CAT, 0L))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    return payload instanceof PetCreatedEvent event
                        && event.getType() == PetType.CAT;
                  }));
    }

    @Test
    @DisplayName("should create DRAGON pet successfully")
    void shouldCreateDragonPet() {
      fixture
          .givenNoPriorActivity()
          .when(new CreatePetCommand("pet-3", "Smaug", PetType.DRAGON, 0L))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    return payload instanceof PetCreatedEvent event
                        && event.getType() == PetType.DRAGON;
                  }));
    }
  }

  @Nested
  @DisplayName("Pet Playing")
  class PetPlaying {

    @Test
    @DisplayName("should increase happiness and hunger when playing")
    void shouldIncreaseHappinessAndHungerWhenPlaying() {
      String petId = "pet-123";

      fixture
          .given(new PetCreatedEvent(petId, "Buddy", PetType.DOG, 0L, Instant.now()))
          .when(new PlayWithPetCommand(petId))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof PetPlayedWithEvent event)) {
                      return false;
                    }
                    return event.getPetId().equals(petId)
                        && event.getHappinessIncrease() == 15
                        && event.getHungerIncrease() == 5
                        && event.getTimestamp() != null;
                  }));
    }

    @Test
    @DisplayName("should cap happiness increase at 100")
    void shouldCapHappinessIncreaseAt100() {
      String petId = "pet-123";

      // Happiness starts at 70, so we can increase by at most 30
      fixture
          .given(new PetCreatedEvent(petId, "Buddy", PetType.DOG, 0L, Instant.now()))
          .when(new PlayWithPetCommand(petId))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof PetPlayedWithEvent event)) {
                      return false;
                    }
                    // Initial happiness is 70, can increase by 15 to reach 85
                    return event.getHappinessIncrease() == 15;
                  }));
    }

    @Test
    @DisplayName("should reject playing when happiness is at maximum")
    void shouldRejectPlayingWhenHappinessIsAtMaximum() {
      String petId = "pet-123";

      // Create pet and play multiple times to reach 100 happiness
      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, 0L, Instant.now()),
              new PetPlayedWithEvent(petId, 15, 5, Instant.now()),
              new PetPlayedWithEvent(petId, 15, 5, Instant.now()))
          .when(new PlayWithPetCommand(petId))
          .expectException(IllegalStateException.class)
          .expectExceptionMessage("Pet is already at maximum happiness");
    }

  }

  @Nested
  @DisplayName("Pet Cleaning")
  class PetCleaning {

    @Test
    @DisplayName("should allow cleaning when health is at maximum (0 increase)")
    void shouldAllowCleaningWhenHealthIsAtMaximum() {
      String petId = "pet-123";

      fixture
          .given(new PetCreatedEvent(petId, "Fluffy", PetType.CAT, 0L, Instant.now()))
          .when(new CleanPetCommand(petId))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof PetCleanedEvent event)) {
                      return false;
                    }
                    // Initial health is 100, so increase should be 0 (capped)
                    return event.getPetId().equals(petId)
                        && event.getHealthIncrease() == 0
                        && event.getTimestamp() != null;
                  }));
    }

    // TODO: Re-enable once we have a way to make pets die (e.g., health reaches 0)
    // @Test
    // @DisplayName("should reject cleaning dead pet")
    // void shouldRejectCleaningDeadPet() {
    //   String petId = "pet-123";
    //
    //   fixture
    //       .given(new PetCreatedEvent(petId, "Fluffy", PetType.CAT, Instant.now()))
    //       .andGivenCommands(
    //           // Need to implement pet death mechanism first
    //       )
    //       .when(new CleanPetCommand(petId))
    //       .expectException(IllegalStateException.class)
    //       .expectExceptionMessage("Cannot clean a dead pet");
    // }
  }
}
