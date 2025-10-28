package com.reactor.pets.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.command.FeedPetCommand;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetStatusView;
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
 * Integration tests for Pet lifecycle flows.
 *
 * <p>These tests verify the complete CQRS flow: - Command dispatching through CommandGateway -
 * Event processing and storage in Axon Server - Projection updates via EventHandlers - Query
 * handling through QueryGateway
 *
 * <p>PREREQUISITES: - Axon Server must be running (docker-compose up -d) - Tests assume backing
 * services are available
 *
 * <p>These are true integration tests that exercise: - Real CommandGateway and QueryGateway - Real
 * event store (Axon Server) - Real event processors and projections - Full Spring context
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Pet Lifecycle Integration")
class PetLifecycleIntegrationTest {

  @Autowired private CommandGateway commandGateway;

  @Autowired private QueryGateway queryGateway;

  @Nested
  @DisplayName("Pet Creation Flow")
  class PetCreationFlow {

    @Test
    @DisplayName("should create pet and query its status")
    void shouldCreatePetAndQueryStatus() throws Exception {
      // Given
      String petId = UUID.randomUUID().toString();
      String petName = "Integration Test Pet";
      PetType petType = PetType.DOG;

      // When: Dispatch CreatePetCommand
      commandGateway.sendAndWait(new CreatePetCommand(petId, petName, petType));

      // Allow time for event processing and projection update
      TimeUnit.MILLISECONDS.sleep(500);

      // Then: Query should return pet with correct initial state
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status).isNotNull();
      assertThat(status.getPetId()).isEqualTo(petId);
      assertThat(status.getName()).isEqualTo(petName);
      assertThat(status.getType()).isEqualTo(petType);
      assertThat(status.getHunger()).isEqualTo(30); // Initial hunger
      assertThat(status.getHappiness()).isEqualTo(70); // Initial happiness
      assertThat(status.getHealth()).isEqualTo(100); // Initial health
      assertThat(status.getStage()).isEqualTo(PetStage.EGG);
      assertThat(status.isAlive()).isTrue();
    }

    @Test
    @DisplayName("should create multiple pets independently")
    void shouldCreateMultiplePetsIndependently() throws Exception {
      // Given
      String petId1 = UUID.randomUUID().toString();
      String petId2 = UUID.randomUUID().toString();

      // When: Create two different pets
      commandGateway.sendAndWait(new CreatePetCommand(petId1, "Dog Pet", PetType.DOG));
      commandGateway.sendAndWait(new CreatePetCommand(petId2, "Cat Pet", PetType.CAT));

      TimeUnit.MILLISECONDS.sleep(500);

      // Then: Both pets should exist with correct attributes
      PetStatusView pet1 =
          queryGateway
              .query(new GetPetStatusQuery(petId1), ResponseTypes.instanceOf(PetStatusView.class))
              .join();
      PetStatusView pet2 =
          queryGateway
              .query(new GetPetStatusQuery(petId2), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(pet1.getName()).isEqualTo("Dog Pet");
      assertThat(pet1.getType()).isEqualTo(PetType.DOG);

      assertThat(pet2.getName()).isEqualTo("Cat Pet");
      assertThat(pet2.getType()).isEqualTo(PetType.CAT);
    }

    @Test
    @DisplayName("should reject query for non-existent pet")
    void shouldRejectQueryForNonExistentPet() {
      // Given: A pet ID that doesn't exist
      String nonExistentPetId = UUID.randomUUID().toString();

      // When/Then: Query should fail
      assertThatThrownBy(
              () ->
                  queryGateway
                      .query(
                          new GetPetStatusQuery(nonExistentPetId),
                          ResponseTypes.instanceOf(PetStatusView.class))
                      .join())
          .hasMessageContaining("Pet not found");
    }
  }

  @Nested
  @DisplayName("Pet Feeding Flow")
  class PetFeedingFlow {

    @Test
    @DisplayName("should feed pet and reduce hunger in projection")
    void shouldFeedPetAndReduceHungerInProjection() throws Exception {
      // Given: A created pet
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Hungry Pet", PetType.CAT));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Feed the pet
      commandGateway.sendAndWait(new FeedPetCommand(petId, 15));
      TimeUnit.MILLISECONDS.sleep(500);

      // Then: Hunger should be reduced
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getHunger()).isEqualTo(15); // 30 - 15 = 15
    }

    @Test
    @DisplayName("should handle multiple feedings correctly")
    void shouldHandleMultipleFeedingsCorrectly() throws Exception {
      // Given: A created pet
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Very Hungry Pet", PetType.DRAGON));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Feed multiple times
      commandGateway.sendAndWait(new FeedPetCommand(petId, 10));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new FeedPetCommand(petId, 10));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new FeedPetCommand(petId, 10));
      TimeUnit.MILLISECONDS.sleep(300);

      // Then: Hunger should be reduced to 0 (30 - 10 - 10 - 10 = 0)
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getHunger()).isEqualTo(0);
    }

    @Test
    @DisplayName("should not reduce hunger below zero")
    void shouldNotReduceHungerBelowZero() throws Exception {
      // Given: A created pet
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Overfed Pet", PetType.DOG));
      TimeUnit.MILLISECONDS.sleep(500);

      // When: Feed with more food than hunger level
      commandGateway.sendAndWait(new FeedPetCommand(petId, 100)); // More than initial 30
      TimeUnit.MILLISECONDS.sleep(500);

      // Then: Hunger should be 0, not negative
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getHunger()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Event Sourcing Verification")
  class EventSourcingVerification {

    @Test
    @DisplayName("should reconstruct aggregate state from events")
    void shouldReconstructAggregateStateFromEvents() throws Exception {
      // Given: Create a pet and perform multiple actions
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Event Sourced Pet", PetType.CAT));
      TimeUnit.MILLISECONDS.sleep(300);

      commandGateway.sendAndWait(new FeedPetCommand(petId, 5));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new FeedPetCommand(petId, 10));
      TimeUnit.MILLISECONDS.sleep(300);
      commandGateway.sendAndWait(new FeedPetCommand(petId, 5));
      TimeUnit.MILLISECONDS.sleep(300);

      // When: Query final state
      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      // Then: State should reflect all events
      // Initial: hunger=30, then -5, -10, -5 = 10
      assertThat(status.getHunger()).isEqualTo(10);
      assertThat(status.getName()).isEqualTo("Event Sourced Pet");
      assertThat(status.getType()).isEqualTo(PetType.CAT);
    }
  }

  @Nested
  @DisplayName("Business Rules Validation")
  class BusinessRulesValidation {

    @Test
    @DisplayName("should reject invalid pet name")
    void shouldRejectInvalidPetName() {
      // When/Then: Empty name should be rejected
      assertThatThrownBy(
              () ->
                  commandGateway.sendAndWait(
                      new CreatePetCommand(UUID.randomUUID().toString(), "", PetType.DOG)))
          .hasMessageContaining("Pet name cannot be empty");
    }

    @Test
    @DisplayName("should reject null pet type")
    void shouldRejectNullPetType() {
      // When/Then: Null type should be rejected
      assertThatThrownBy(
              () ->
                  commandGateway.sendAndWait(
                      new CreatePetCommand(UUID.randomUUID().toString(), "Invalid Pet", null)))
          .hasMessageContaining("Pet type cannot be null");
    }

    @Test
    @DisplayName("should reject invalid food amount")
    void shouldRejectInvalidFoodAmount() throws Exception {
      // Given: A created pet
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Test Pet", PetType.DOG));
      TimeUnit.MILLISECONDS.sleep(500);

      // When/Then: Zero or negative food should be rejected
      assertThatThrownBy(() -> commandGateway.sendAndWait(new FeedPetCommand(petId, 0)))
          .hasMessageContaining("Food amount must be positive");

      assertThatThrownBy(() -> commandGateway.sendAndWait(new FeedPetCommand(petId, -10)))
          .hasMessageContaining("Food amount must be positive");
    }
  }

  @Nested
  @DisplayName("All Pet Types")
  class AllPetTypesIntegration {

    @Test
    @DisplayName("should create and manage DOG pet")
    void shouldCreateAndManageDogPet() throws Exception {
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Buddy", PetType.DOG));
      TimeUnit.MILLISECONDS.sleep(500);

      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getType()).isEqualTo(PetType.DOG);
    }

    @Test
    @DisplayName("should create and manage CAT pet")
    void shouldCreateAndManageCatPet() throws Exception {
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Whiskers", PetType.CAT));
      TimeUnit.MILLISECONDS.sleep(500);

      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getType()).isEqualTo(PetType.CAT);
    }

    @Test
    @DisplayName("should create and manage DRAGON pet")
    void shouldCreateAndManageDragonPet() throws Exception {
      String petId = UUID.randomUUID().toString();
      commandGateway.sendAndWait(new CreatePetCommand(petId, "Smaug", PetType.DRAGON));
      TimeUnit.MILLISECONDS.sleep(500);

      PetStatusView status =
          queryGateway
              .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
              .join();

      assertThat(status.getType()).isEqualTo(PetType.DRAGON);
    }
  }
}
