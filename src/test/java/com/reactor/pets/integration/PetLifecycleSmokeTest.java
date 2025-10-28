package com.reactor.pets.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke tests for basic Pet lifecycle integration.
 *
 * <p>These are simplified integration tests that verify the core CQRS flow works with minimal
 * waiting.
 *
 * <p>PREREQUISITES: - Axon Server must be running (docker-compose up -d)
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Pet Lifecycle Smoke Test")
class PetLifecycleSmokeTest {

  @Autowired private CommandGateway commandGateway;

  @Autowired private QueryGateway queryGateway;

  @Test
  @DisplayName("should create pet and query its status")
  void shouldCreatePetAndQueryStatus() throws Exception {
    // Given
    String petId = UUID.randomUUID().toString();
    String petName = "Smoke Test Pet";
    PetType petType = PetType.DOG;

    // When: Dispatch CreatePetCommand
    commandGateway.sendAndWait(new CreatePetCommand(petId, petName, petType));

    // Allow time for event processing (events typically process in <10ms)
    TimeUnit.MILLISECONDS.sleep(50);

    // Then: Query should return pet with correct initial state
    PetStatusView status =
        queryGateway
            .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
            .join();

    assertThat(status).isNotNull();
    assertThat(status.getPetId()).isEqualTo(petId);
    assertThat(status.getName()).isEqualTo(petName);
    assertThat(status.getType()).isEqualTo(petType);
    assertThat(status.getHunger()).isEqualTo(30);
    assertThat(status.getHappiness()).isEqualTo(70);
    assertThat(status.getHealth()).isEqualTo(100);
    assertThat(status.getStage()).isEqualTo(PetStage.EGG);
    assertThat(status.isAlive()).isTrue();
  }

  @Test
  @DisplayName("should feed pet and reduce hunger")
  void shouldFeedPetAndReduceHunger() throws Exception {
    // Given: A created pet
    String petId = UUID.randomUUID().toString();
    commandGateway.sendAndWait(new CreatePetCommand(petId, "Hungry Pet", PetType.CAT));
    TimeUnit.MILLISECONDS.sleep(50);

    // When: Feed the pet
    commandGateway.sendAndWait(new FeedPetCommand(petId, 15));
    TimeUnit.MILLISECONDS.sleep(50);

    // Then: Hunger should be reduced
    PetStatusView status =
        queryGateway
            .query(new GetPetStatusQuery(petId), ResponseTypes.instanceOf(PetStatusView.class))
            .join();

    assertThat(status.getHunger()).isEqualTo(15); // 30 - 15 = 15
  }

  @Test
  @DisplayName("should handle all pet types")
  void shouldHandleAllPetTypes() throws Exception {
    // Test DOG
    String dogId = UUID.randomUUID().toString();
    commandGateway.sendAndWait(new CreatePetCommand(dogId, "Dog", PetType.DOG));
    TimeUnit.MILLISECONDS.sleep(50);

    PetStatusView dogStatus =
        queryGateway
            .query(new GetPetStatusQuery(dogId), ResponseTypes.instanceOf(PetStatusView.class))
            .join();
    assertThat(dogStatus.getType()).isEqualTo(PetType.DOG);

    // Test CAT
    String catId = UUID.randomUUID().toString();
    commandGateway.sendAndWait(new CreatePetCommand(catId, "Cat", PetType.CAT));
    TimeUnit.MILLISECONDS.sleep(50);

    PetStatusView catStatus =
        queryGateway
            .query(new GetPetStatusQuery(catId), ResponseTypes.instanceOf(PetStatusView.class))
            .join();
    assertThat(catStatus.getType()).isEqualTo(PetType.CAT);

    // Test DRAGON
    String dragonId = UUID.randomUUID().toString();
    commandGateway.sendAndWait(new CreatePetCommand(dragonId, "Dragon", PetType.DRAGON));
    TimeUnit.MILLISECONDS.sleep(50);

    PetStatusView dragonStatus =
        queryGateway
            .query(new GetPetStatusQuery(dragonId), ResponseTypes.instanceOf(PetStatusView.class))
            .join();
    assertThat(dragonStatus.getType()).isEqualTo(PetType.DRAGON);
  }
}
