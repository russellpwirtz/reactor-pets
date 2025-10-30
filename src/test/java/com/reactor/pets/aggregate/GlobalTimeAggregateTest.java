package com.reactor.pets.aggregate;

import static org.axonframework.test.matchers.Matchers.matches;

import com.reactor.pets.command.AdvanceGlobalTimeCommand;
import com.reactor.pets.command.CreateGlobalTimeCommand;
import com.reactor.pets.event.GlobalTimeAdvancedEvent;
import com.reactor.pets.event.GlobalTimeCreatedEvent;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for GlobalTimeAggregate using Axon's AggregateTestFixture.
 *
 * <p>These tests verify:
 * - GlobalTime creation with singleton ID
 * - Time advancement logic
 * - Idempotency and invariants
 */
@DisplayName("GlobalTime Aggregate")
class GlobalTimeAggregateTest {

  private FixtureConfiguration<GlobalTimeAggregate> fixture;

  @BeforeEach
  void setUp() {
    fixture = new AggregateTestFixture<>(GlobalTimeAggregate.class);
  }

  @Nested
  @DisplayName("GlobalTime Creation")
  class GlobalTimeCreation {

    @Test
    @DisplayName("should create GlobalTime with singleton ID")
    void shouldCreateGlobalTimeWithSingletonId() {
      String timeId = GlobalTimeAggregate.GLOBAL_TIME_ID;

      fixture
          .givenNoPriorActivity()
          .when(new CreateGlobalTimeCommand(timeId))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof GlobalTimeCreatedEvent event)) {
                      return false;
                    }
                    return event.getTimeId().equals(timeId)
                        && event.getTimestamp() != null;
                  }));
    }

    @Test
    @DisplayName("should reject creation with non-singleton ID")
    void shouldRejectCreationWithNonSingletonId() {
      fixture
          .givenNoPriorActivity()
          .when(new CreateGlobalTimeCommand("WRONG_ID"))
          .expectException(IllegalArgumentException.class)
          .expectExceptionMessage("GlobalTime must use ID: " + GlobalTimeAggregate.GLOBAL_TIME_ID);
    }
  }

  @Nested
  @DisplayName("Time Advancement")
  class TimeAdvancement {

    @Test
    @DisplayName("should advance time from tick 0 to tick 1")
    void shouldAdvanceTimeFromZeroToOne() {
      String timeId = GlobalTimeAggregate.GLOBAL_TIME_ID;

      fixture
          .given(new GlobalTimeCreatedEvent(timeId, java.time.Instant.now()))
          .when(new AdvanceGlobalTimeCommand(timeId))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof GlobalTimeAdvancedEvent event)) {
                      return false;
                    }
                    return event.getTimeId().equals(timeId)
                        && event.getNewGlobalTick() == 1L
                        && event.getTimestamp() != null;
                  }));
    }

    @Test
    @DisplayName("should advance time sequentially through multiple ticks")
    void shouldAdvanceTimeSequentially() {
      String timeId = GlobalTimeAggregate.GLOBAL_TIME_ID;

      fixture
          .given(
              new GlobalTimeCreatedEvent(timeId, java.time.Instant.now()),
              new GlobalTimeAdvancedEvent(timeId, 1L, java.time.Instant.now()),
              new GlobalTimeAdvancedEvent(timeId, 2L, java.time.Instant.now()),
              new GlobalTimeAdvancedEvent(timeId, 3L, java.time.Instant.now()))
          .when(new AdvanceGlobalTimeCommand(timeId))
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof GlobalTimeAdvancedEvent event)) {
                      return false;
                    }
                    return event.getNewGlobalTick() == 4L;
                  }));
    }
  }

  @Nested
  @DisplayName("Event Sourcing")
  class EventSourcing {

    @Test
    @DisplayName("should correctly replay events to rebuild state")
    void shouldReplayEventsToRebuildState() {
      String timeId = GlobalTimeAggregate.GLOBAL_TIME_ID;

      // Given a series of events that have occurred
      fixture
          .given(
              new GlobalTimeCreatedEvent(timeId, java.time.Instant.now()),
              new GlobalTimeAdvancedEvent(timeId, 1L, java.time.Instant.now()),
              new GlobalTimeAdvancedEvent(timeId, 2L, java.time.Instant.now()))
          // When we advance time again
          .when(new AdvanceGlobalTimeCommand(timeId))
          // Then the aggregate should have the correct state (tick 2) and advance to tick 3
          .expectSuccessfulHandlerExecution()
          .expectEventsMatching(
              matches(
                  events -> {
                    if (events.size() != 1) {
                      return false;
                    }
                    EventMessage<?> eventMsg = (EventMessage<?>) events.get(0);
                    Object payload = eventMsg.getPayload();
                    if (!(payload instanceof GlobalTimeAdvancedEvent event)) {
                      return false;
                    }
                    return event.getNewGlobalTick() == 3L;
                  }));
    }
  }
}
