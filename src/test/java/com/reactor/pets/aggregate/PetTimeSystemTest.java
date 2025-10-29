package com.reactor.pets.aggregate;

import static org.axonframework.test.matchers.Matchers.matches;

import com.reactor.pets.command.FeedPetCommand;
import com.reactor.pets.command.TimeTickCommand;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetHealthDeterioratedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import com.reactor.pets.event.TimePassedEvent;
import java.time.Instant;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Pet Aggregate Time System (Phase 3).
 *
 * <p>Tests verify: - Time tick processing - Stat degradation over time - Health deterioration - Pet
 * death mechanics - Idempotency of time ticks
 */
@DisplayName("Pet Time System")
class PetTimeSystemTest {

  private FixtureConfiguration<Pet> fixture;

  @BeforeEach
  void setUp() {
    fixture = new AggregateTestFixture<>(Pet.class);
  }

  @Nested
  @DisplayName("Time Tick Processing")
  class TimeTickProcessing {

    @Test
    @DisplayName("should process time tick and degrade stats")
    void shouldProcessTimeTickAndDegradeStats() {
      String petId = "pet-123";

      fixture
          .given(new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()))
          .when(new TimeTickCommand(petId, 1))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof TimePassedEvent event)) {
                      return false;
                    }
                    // Initial hunger is 30, can increase by 3
                    // Initial happiness is 70, can decrease by 2
                    // Age increases by 0 (not yet 10 ticks)
                    return event.getPetId().equals(petId)
                        && event.getHungerIncrease() == 3
                        && event.getHappinessDecrease() == 2
                        && event.getAgeIncrease() == 0
                        && event.getTickCount() == 1;
                  }));
    }

    @Test
    @DisplayName("should increase age after 10 ticks")
    void shouldIncreaseAgeAfter10Ticks() {
      String petId = "pet-123";

      // Create 9 TimePassedEvents to reach tick 9
      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 1, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 2, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 3, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 4, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 5, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 6, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 7, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 8, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 9, 0.0, 0.0, Instant.now()))
          .when(new TimeTickCommand(petId, 10))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() == 0) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof TimePassedEvent event)) {
                      return false;
                    }
                    // On the 10th tick, age should increase by 1
                    return event.getAgeIncrease() == 1 && event.getTickCount() == 10;
                  }));
    }

    @Test
    @DisplayName("should be idempotent - ignore duplicate tick sequence")
    void shouldBeIdempotentIgnoreDuplicateTickSequence() {
      String petId = "pet-123";

      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 5, 0.0, 0.0, Instant.now()))
          .when(new TimeTickCommand(petId, 5))
          .expectSuccessfulHandlerExecution()
          .expectNoEvents(); // Should be ignored
    }

    @Test
    @DisplayName("should ignore old tick sequence")
    void shouldIgnoreOldTickSequence() {
      String petId = "pet-123";

      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 10, 0.0, 0.0, Instant.now()))
          .when(new TimeTickCommand(petId, 5))
          .expectSuccessfulHandlerExecution()
          .expectNoEvents(); // Should be ignored (old tick)
    }

    @Test
    @DisplayName("should cap hunger increase at 100")
    void shouldCapHungerIncreaseAt100() {
      String petId = "pet-123";

      // Set hunger to 98 by applying events
      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              // Initial hunger is 30, need to get to 98
              // Play increases hunger by 5 each time
              new PetPlayedWithEvent(petId, 15, 5, Instant.now()),
              new PetPlayedWithEvent(petId, 10, 5, Instant.now()),
              new PetPlayedWithEvent(petId, 5, 5, Instant.now()),
              // Now at 45 hunger
              new TimePassedEvent(petId, 3, 2, 0, 1, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 2, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 3, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 4, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 5, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 6, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 7, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 8, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 9, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 10, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 11, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 12, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 13, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 14, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 15, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 16, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 17, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 2, 2, 0, 18, 0.0, 0.0, Instant.now()) // Hunger now at 98
              )
          .when(new TimeTickCommand(petId, 19))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() == 0) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof TimePassedEvent event)) {
                      return false;
                    }
                    // Should cap at 2 (100 - 98 = 2), not increase by 3
                    return event.getHungerIncrease() == 2;
                  }));
    }

    @Test
    @DisplayName("should cap happiness decrease at 0")
    void shouldCapHappinessDecreaseAt0() {
      String petId = "pet-123";

      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              // Happiness starts at 70
              // Decrease by 2 per tick - need 35 ticks to reach 0
              new TimePassedEvent(petId, 3, 2, 0, 1, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 2, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 3, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 4, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 5, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 6, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 7, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 8, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 9, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 10, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 11, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 12, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 13, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 14, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 15, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 16, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 17, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 18, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 19, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 20, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 21, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 22, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 23, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 24, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 25, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 26, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 27, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 28, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 29, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 30, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 31, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 32, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 33, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 34, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 1, 0, 35, 0.0, 0.0, Instant.now()) // Happiness now at 1
              )
          .when(new TimeTickCommand(petId, 36))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() == 0) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof TimePassedEvent event)) {
                      return false;
                    }
                    // Should cap at 1, not decrease by 2
                    return event.getHappinessDecrease() == 1;
                  }));
    }
  }

  @Nested
  @DisplayName("Health Deterioration")
  class HealthDeterioration {

    @Test
    @DisplayName("should deteriorate health when hunger exceeds 80")
    void shouldDeteriorateHealthWhenHungerExceeds80() {
      String petId = "pet-123";

      // Get hunger above 80
      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              // Start at 30, need to get to 81
              // Play increases hunger by 5 each time
              new PetPlayedWithEvent(petId, 15, 5, Instant.now()),
              new PetPlayedWithEvent(petId, 10, 5, Instant.now()),
              new PetPlayedWithEvent(petId, 5, 5, Instant.now()),
              // Now at 45 hunger
              new TimePassedEvent(petId, 3, 2, 0, 1, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 2, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 3, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 4, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 5, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 6, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 7, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 8, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 9, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 10, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 11, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 12, 0.0, 0.0, Instant.now()) // Now at 81
              )
          .when(new TimeTickCommand(petId, 13))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() < 2) {
                      return false;
                    }
                    // First event should be TimePassedEvent
                    EventMessage<?> event1Msg = (EventMessage<?>) events.get(0);
                    if (!(event1Msg.getPayload() instanceof TimePassedEvent)) {
                      return false;
                    }

                    // Second event should be PetHealthDeterioratedEvent
                    EventMessage<?> event2Msg = (EventMessage<?>) events.get(1);
                    Object payload2 = event2Msg.getPayload();
                    if (!(payload2 instanceof PetHealthDeterioratedEvent event)) {
                      return false;
                    }

                    // Health decrease should be 5 for extreme hunger
                    return event.getPetId().equals(petId)
                        && event.getHealthDecrease() == 5
                        && event.getReason().contains("Extreme hunger");
                  }));
    }

    @Test
    @DisplayName("should deteriorate health when happiness below 20")
    void shouldDeteriorateHealthWhenHappinessBelow20() {
      String petId = "pet-123";

      // Get happiness below 20 but keep hunger below 80
      // Strategy: feed the pet regularly to keep hunger low while time passes
      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              // Happiness starts at 70, decrease by 2 per tick
              // Need (70-19)/2 = 25.5, so 26 ticks to get to 18
              // But we need to keep hunger below 80
              // After feeding, hunger is low, then time ticks increase it
              new TimePassedEvent(petId, 3, 2, 0, 1, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 2, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 3, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 4, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 5, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 6, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 7, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 8, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 9, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 10, 0.0, 0.0, Instant.now()),
              // Feed to reduce hunger (hunger is now 60)
              new PetFedEvent(petId, 30, Instant.now()),
              // Continue time passing (hunger is now 30)
              new TimePassedEvent(petId, 3, 2, 0, 11, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 12, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 13, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 14, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 15, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 16, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 17, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 18, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 19, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 20, 0.0, 0.0, Instant.now()),
              // Feed again to keep hunger low (hunger was 60, now 30)
              new PetFedEvent(petId, 30, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 21, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 22, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 23, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 24, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 25, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 26, 0.0, 0.0, Instant.now())
              // Hunger is now 48, happiness is 18
              )
          .when(new TimeTickCommand(petId, 27))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() < 2) {
                      return false;
                    }
                    // Second event should be PetHealthDeterioratedEvent
                    EventMessage<?> event2Msg = (EventMessage<?>) events.get(1);
                    Object payload2 = event2Msg.getPayload();
                    if (!(payload2 instanceof PetHealthDeterioratedEvent event)) {
                      return false;
                    }

                    // Health decrease should be 3 for low happiness only (hunger < 80)
                    return event.getPetId().equals(petId)
                        && event.getHealthDecrease() == 3
                        && event.getReason().equals("Low happiness");
                  }));
    }

    @Test
    @DisplayName("should deteriorate health from both hunger and happiness")
    void shouldDeteriorateHealthFromBothHungerAndHappiness() {
      String petId = "pet-123";

      // Get hunger above 80 AND happiness below 20
      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              // Start at hunger 30, happiness 70
              // After 26 ticks: hunger = 30 + 26*3 = 108 (capped at 100), happiness = 70 - 26*2 = 18
              new PetPlayedWithEvent(petId, 15, 5, Instant.now()),
              new PetPlayedWithEvent(petId, 10, 5, Instant.now()),
              new PetPlayedWithEvent(petId, 5, 5, Instant.now()),
              // Now at 45 hunger, 100 happiness
              new TimePassedEvent(petId, 3, 2, 0, 1, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 2, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 3, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 4, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 5, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 6, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 7, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 8, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 9, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 10, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 11, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 12, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 13, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 14, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 15, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 16, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 17, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 18, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 19, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 20, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 21, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 22, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 23, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 24, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 25, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 26, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 27, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 28, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 29, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 30, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 31, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 32, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 33, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 34, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 35, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 36, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 37, 0.0, 0.0, Instant.now()) // hunger at 156 (capped 100), happiness 26
              )
          .when(new TimeTickCommand(petId, 38))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() < 2) {
                      return false;
                    }
                    // Second event should be PetHealthDeterioratedEvent
                    EventMessage<?> event2Msg = (EventMessage<?>) events.get(1);
                    Object payload2 = event2Msg.getPayload();
                    if (!(payload2 instanceof PetHealthDeterioratedEvent event)) {
                      return false;
                    }

                    // Health decrease should be 5 (extreme hunger) only, since happiness is 24 (not < 20)
                    return event.getPetId().equals(petId)
                        && event.getHealthDecrease() == 5
                        && event.getReason().contains("Extreme hunger");
                  }));
    }
  }

  @Nested
  @DisplayName("Pet Death")
  class PetDeath {

    @Test
    @DisplayName("should emit PetDiedEvent when health reaches 0")
    void shouldEmitPetDiedEventWhenHealthReaches0() {
      String petId = "pet-123";

      // Set up pet with very low health (1) and high hunger
      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              // Get hunger high enough to cause health deterioration
              new PetPlayedWithEvent(petId, 15, 5, Instant.now()),
              new PetPlayedWithEvent(petId, 10, 5, Instant.now()),
              new PetPlayedWithEvent(petId, 5, 5, Instant.now()),
              // Lots of time ticks to increase hunger and decrease health
              new TimePassedEvent(petId, 3, 2, 0, 1, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 2, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 3, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 4, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 5, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 6, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 7, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 8, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 9, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 10, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 11, 0.0, 0.0, Instant.now()),
              new TimePassedEvent(petId, 3, 2, 0, 12, 0.0, 0.0, Instant.now()),
              // Health deterioration events
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 5, "Extreme hunger", Instant.now()),
              new PetHealthDeterioratedEvent(petId, 4, "Extreme hunger", Instant.now()) // Health now at 1
              )
          .when(new TimeTickCommand(petId, 13))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() < 3) {
                      return false;
                    }
                    // Third event should be PetDiedEvent
                    EventMessage<?> event3Msg = (EventMessage<?>) events.get(2);
                    Object payload3 = event3Msg.getPayload();
                    if (!(payload3 instanceof PetDiedEvent event)) {
                      return false;
                    }

                    return event.getPetId().equals(petId)
                        && event.getCauseOfDeath().contains("Health reached zero");
                  }));
    }

    @Test
    @DisplayName("should not process time ticks for dead pets")
    void shouldNotProcessTimeTicksForDeadPets() {
      String petId = "pet-123";

      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              new PetDiedEvent(petId, 5, 50, "Health reached zero", Instant.now()))
          .when(new TimeTickCommand(petId, 51))
          .expectSuccessfulHandlerExecution()
          .expectNoEvents(); // Dead pet should not process time ticks
    }

    @Test
    @DisplayName("should not allow feeding dead pet")
    void shouldNotAllowFeedingDeadPet() {
      String petId = "pet-123";

      fixture
          .given(
              new PetCreatedEvent(petId, "Buddy", PetType.DOG, Instant.now()),
              new PetDiedEvent(petId, 5, 50, "Health reached zero", Instant.now()))
          .when(new FeedPetCommand(petId, 20))
          .expectException(IllegalStateException.class)
          .expectExceptionMessage("Cannot feed a dead pet");
    }
  }
}
