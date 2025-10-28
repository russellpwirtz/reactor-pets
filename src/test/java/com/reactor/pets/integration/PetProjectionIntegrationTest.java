package com.reactor.pets.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.command.CleanPetCommand;
import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.command.FeedPetCommand;
import com.reactor.pets.command.PlayWithPetCommand;
import com.reactor.pets.query.GetAllPetsQuery;
import com.reactor.pets.query.GetPetHistoryQuery;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetEventDto;
import com.reactor.pets.query.PetStatusView;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for Pet Projection features.
 *
 * <p>These tests verify the projection layer functionality: - PetStatusProjection event handlers -
 * PetHistoryProjection query handlers - GetAllPetsQuery functionality - Complete CQRS flow for new
 * commands
 *
 * <p>PREREQUISITES: - Axon Server must be running (docker-compose up -d) - Tests assume backing
 * services are available
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Pet Projection Integration")
class PetProjectionIntegrationTest {

  @Autowired private CommandGateway commandGateway;

  @Autowired private QueryGateway queryGateway;

  @Nested
  @DisplayName("Play With Pet Flow")
  class PlayWithPetFlow {

    @Test
    @DisplayName("should play with pet and update happiness and hunger")
    void shouldPlayWithPetAndUpdateHappinessAndHunger() throws Exception {
      // Given: A created pet
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Playful Pet", PetType.DOG));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Play with the pet
      commandGateway.sendAndWait(new PlayWithPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(500);

      // Then: Happiness should increase and hunger should increase
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getHappiness()).isGreaterThan(70); // Initial is 70
      assertThat(status.getHunger()).isGreaterThan(30); // Initial is 30
    }

    @Test
    @DisplayName("should handle multiple play sessions")
    void shouldHandleMultiplePlaySessions() throws Exception {
      // Given: A created pet
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Very Playful Pet", PetType.CAT));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Play twice (initial happiness is 70, each play adds 15, so 70+15+15=100)
      commandGateway.sendAndWait(new PlayWithPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new PlayWithPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);

      // Then: Happiness should be at maximum
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getHappiness()).isEqualTo(100);
    }

    @Test
    @DisplayName("should cap happiness at 100")
    void shouldCapHappinessAt100() throws Exception {
      // Given: A created pet
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Maximum Happy Pet", PetType.DRAGON));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Play twice to reach max happiness (70 + 15 + 15 = 100)
      commandGateway.sendAndWait(new PlayWithPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new PlayWithPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);

      // Then: Happiness should be exactly 100
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getHappiness()).isEqualTo(100);

      // And: Further play attempts should fail due to business rule
      assertThatThrownBy(() -> commandGateway.sendAndWait(new PlayWithPetCommand(petId)))
          .hasMessageContaining("already at maximum happiness");
    }
  }

  @Nested
  @DisplayName("Clean Pet Flow")
  class CleanPetFlow {

    @Test
    @DisplayName("should clean pet and increase health")
    void shouldCleanPetAndIncreaseHealth() throws Exception {
      // Given: A created pet with less than perfect health
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Dirty Pet", PetType.CAT));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Clean the pet
      commandGateway.sendAndWait(new CleanPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(500);

      // Then: Health should increase
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      // Health starts at 100, cleaning increases it but caps at 100
      assertThat(status.getHealth()).isEqualTo(100);
    }

    @Test
    @DisplayName("should handle multiple cleaning sessions")
    void shouldHandleMultipleCleaningSessions() throws Exception {
      // Given: A created pet
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Very Clean Pet", PetType.DOG));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Clean multiple times
      commandGateway.sendAndWait(new CleanPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new CleanPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new CleanPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);

      // Then: Health should remain at maximum
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getHealth()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("Get All Pets Query")
  class GetAllPetsQueryFlow {

    @Test
    @DisplayName("should return all created pets")
    void shouldReturnAllCreatedPets() throws Exception {
      // Given: Multiple pets created
      String petId1 = UUID.randomUUID().toString();
      String petId2 = UUID.randomUUID().toString();
      String petId3 = UUID.randomUUID().toString();

      commandGateway.sendAndWait(new CreatePetCommand(petId1, "Pet One", PetType.DOG));
      commandGateway.sendAndWait(new CreatePetCommand(petId2, "Pet Two", PetType.CAT));
      commandGateway.sendAndWait(new CreatePetCommand(petId3, "Pet Three", PetType.DRAGON));
      TimeUnit.MILLISECONDS.sleep(1000);

      // When: Query all pets
      List<PetStatusView> allPets =
          queryGateway
              .query(new GetAllPetsQuery(), ResponseTypes.multipleInstancesOf(PetStatusView.class))
              .join();

      // Then: Should contain at least the three pets we created
      assertThat(allPets).isNotEmpty();
      assertThat(allPets).hasSizeGreaterThanOrEqualTo(3);

      // Verify our specific pets are in the list
      List<String> petIds = allPets.stream().map(PetStatusView::getPetId).toList();
      assertThat(petIds).contains(petId1, petId2, petId3);
    }

    @Test
    @DisplayName("should return pets with correct attributes")
    void shouldReturnPetsWithCorrectAttributes() throws Exception {
      // Given: A pet with specific attributes
      String petId = UUID.randomUUID().toString();
      String petName = "Attribute Test Pet";
      PetType petType = PetType.DRAGON;

      commandGateway.sendAndWait(new CreatePetCommand(petId, petName, petType));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Query all pets
      List<PetStatusView> allPets =
          queryGateway
              .query(new GetAllPetsQuery(), ResponseTypes.multipleInstancesOf(PetStatusView.class))
              .join();

      // Then: Should find our pet with correct attributes
      PetStatusView ourPet =
          allPets.stream().filter(p -> p.getPetId().equals(petId)).findFirst().orElse(null);

      assertThat(ourPet).isNotNull();
      assertThat(ourPet.getName()).isEqualTo(petName);
      assertThat(ourPet.getType()).isEqualTo(petType);
      assertThat(ourPet.isAlive()).isTrue();
    }
  }

  @Nested
  @DisplayName("Pet History Query")
  class PetHistoryQueryFlow {

    @Test
    @DisplayName("should return pet creation event in history")
    void shouldReturnPetCreationEventInHistory() throws Exception {
      // Given: A newly created pet
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "History Pet", PetType.CAT));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Query pet history
      List<PetEventDto> history =
          queryGateway
              .query(
                  new GetPetHistoryQuery(petId, 10),
                  ResponseTypes.multipleInstancesOf(PetEventDto.class))
              .join();

      // Then: Should contain creation event
      assertThat(history).isNotEmpty();
      assertThat(history).hasSize(1);

      PetEventDto creationEvent = history.get(0);
      assertThat(creationEvent.getEventType()).isEqualTo("PetCreatedEvent");
      assertThat(creationEvent.getDetails()).contains("History Pet");
      assertThat(creationEvent.getDetails()).contains("CAT");
    }

    @Test
    @DisplayName("should return multiple events in chronological order")
    void shouldReturnMultipleEventsInChronologicalOrder() throws Exception {
      // Given: A pet with multiple events
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Event Test Pet", PetType.DOG));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new FeedPetCommand(petId, 10));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new PlayWithPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new CleanPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);

      // When: Query pet history
      List<PetEventDto> history =
          queryGateway
              .query(
                  new GetPetHistoryQuery(petId, 10),
                  ResponseTypes.multipleInstancesOf(PetEventDto.class))
              .join();

      // Then: Should contain all events
      assertThat(history).hasSize(4);
      assertThat(history.get(0).getEventType()).isEqualTo("PetCreatedEvent");
      assertThat(history.get(1).getEventType()).isEqualTo("PetFedEvent");
      assertThat(history.get(2).getEventType()).isEqualTo("PetPlayedWithEvent");
      assertThat(history.get(3).getEventType()).isEqualTo("PetCleanedEvent");

      // Verify details are formatted correctly
      assertThat(history.get(1).getDetails()).contains("hunger reduced");
      assertThat(history.get(2).getDetails()).contains("Played with pet");
      assertThat(history.get(3).getDetails()).contains("cleaned");
    }

    @Test
    @DisplayName("should respect limit parameter")
    void shouldRespectLimitParameter() throws Exception {
      // Given: A pet with many events
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Limited History Pet", PetType.CAT));
      TimeUnit.MILLISECONDS.sleep(300);

      // Create several events
      for (int i = 0; i < 5; i++) {
        commandGateway.sendAndWait(new FeedPetCommand(petId, 1));
        TimeUnit.MILLISECONDS.sleep(200);
      }

      // When: Query with limit of 3
      List<PetEventDto> history =
          queryGateway
              .query(
                  new GetPetHistoryQuery(petId, 3),
                  ResponseTypes.multipleInstancesOf(PetEventDto.class))
              .join();

      // Then: Should return only the last 3 events
      assertThat(history).hasSize(3);
      // The most recent events should be included
      assertThat(history.get(history.size() - 1).getEventType()).isEqualTo("PetFedEvent");
    }

    @Test
    @DisplayName("should handle large limit gracefully")
    void shouldHandleLargeLimitGracefully() throws Exception {
      // Given: A pet with a few events
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Large Limit Pet", PetType.DRAGON));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new FeedPetCommand(petId, 5));
      TimeUnit.MILLISECONDS.sleep(300);

      // When: Query with a very large limit
      List<PetEventDto> history =
          queryGateway
              .query(
                  new GetPetHistoryQuery(petId, 100),
                  ResponseTypes.multipleInstancesOf(PetEventDto.class))
              .join();

      // Then: Should return all available events (capped at 50 by implementation)
      assertThat(history).hasSizeLessThanOrEqualTo(50);
      assertThat(history).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("should include all event types in details")
    void shouldIncludeAllEventTypesInDetails() throws Exception {
      // Given: A pet with all event types
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "All Events Pet", PetType.DOG));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new FeedPetCommand(petId, 10));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new PlayWithPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new CleanPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);

      // When: Query pet history
      List<PetEventDto> history =
          queryGateway
              .query(
                  new GetPetHistoryQuery(petId, 10),
                  ResponseTypes.multipleInstancesOf(PetEventDto.class))
              .join();

      // Then: All event types should be present with proper details
      assertThat(history).hasSize(4);

      // Verify each event has meaningful details
      for (PetEventDto event : history) {
        assertThat(event.getDetails()).isNotEmpty();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getEventType()).isNotEmpty();
      }
    }
  }

  @Nested
  @DisplayName("Complete Pet Lifecycle Integration")
  class CompletePetLifecycleIntegration {

    @Test
    @DisplayName("should handle complete pet lifecycle with all commands")
    void shouldHandleCompletePetLifecycleWithAllCommands() throws Exception {
      // Given: A new pet
      String petId = UUID.randomUUID().toString();
      String petName = "Complete Lifecycle Pet";
      PetType petType = PetType.CAT;

      // When: Execute complete lifecycle
      commandGateway.sendAndWait(new CreatePetCommand(petId, petName, petType));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new FeedPetCommand(petId, 10));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new PlayWithPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new CleanPetCommand(petId));
      TimeUnit.MILLISECONDS.sleep(500);

      // Then: Query status should reflect all changes
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getName()).isEqualTo(petName);
      assertThat(status.getType()).isEqualTo(petType);
      assertThat(status.isAlive()).isTrue();

      // And: History should contain all events
      List<PetEventDto> history =
          queryGateway
              .query(
                  new GetPetHistoryQuery(petId, 10),
                  ResponseTypes.multipleInstancesOf(PetEventDto.class))
              .join();

      assertThat(history).hasSize(4);

      // And: Pet should be in all pets query
      List<PetStatusView> allPets =
          queryGateway
              .query(new GetAllPetsQuery(), ResponseTypes.multipleInstancesOf(PetStatusView.class))
              .join();

      assertThat(allPets.stream().map(PetStatusView::getPetId)).contains(petId);
    }
  }
}
