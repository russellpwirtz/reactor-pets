package com.reactor.pets.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetStatusRepository;
import com.reactor.pets.query.PetStatusView;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for PetStatusProjection.
 *
 * <p>
 * These tests verify:
 * - Event handler logic for projection updates
 * - Query handler logic
 * - Pet status view construction
 * - Edge cases and error handling
 */
@DisplayName("Pet Status Projection")
@ExtendWith(MockitoExtension.class)
class PetStatusProjectionTest {

  @Mock
  private PetStatusRepository petStatusRepository;

  private PetStatusProjection projection;

  // In-memory store to simulate repository behavior for tests
  private Map<String, PetStatusView> testStore;

  @BeforeEach
  void setUp() {
    testStore = new HashMap<>();
    projection = new PetStatusProjection(petStatusRepository);

    // Setup mock behavior with lenient() to avoid unnecessary stubbing errors
    // Some tests may not use all stubs, which is fine
    lenient().when(petStatusRepository.save(any(PetStatusView.class)))
        .thenAnswer(invocation -> {
          PetStatusView view = invocation.getArgument(0);
          testStore.put(view.getPetId(), view);
          return view;
        });

    lenient().when(petStatusRepository.findById(any(String.class)))
        .thenAnswer(invocation -> {
          String petId = invocation.getArgument(0);
          return Optional.ofNullable(testStore.get(petId));
        });
  }

  @Nested
  @DisplayName("Pet Created Event Handling")
  class PetCreatedEventHandling {

    @Test
    @DisplayName("should create pet status view with correct initial values")
    void shouldCreatePetStatusViewWithCorrectInitialValues() {
      // Given
      String petId = "pet-123";
      String petName = "Fluffy";
      PetType petType = PetType.CAT;
      PetCreatedEvent event = new PetCreatedEvent(petId, petName, petType, Instant.now());

      // When
      projection.on(event);

      // Then
      PetStatusView view = projection.handle(new GetPetStatusQuery(petId));
      assertThat(view).isNotNull();
      assertThat(view.getPetId()).isEqualTo(petId);
      assertThat(view.getName()).isEqualTo(petName);
      assertThat(view.getType()).isEqualTo(petType);
      assertThat(view.getHunger()).isEqualTo(30);
      assertThat(view.getHappiness()).isEqualTo(70);
      assertThat(view.getHealth()).isEqualTo(100);
      assertThat(view.getStage()).isEqualTo(PetStage.EGG);
      assertThat(view.isAlive()).isTrue();
    }

    @Test
    @DisplayName("should handle multiple pet creations independently")
    void shouldHandleMultiplePetCreationsIndependently() {
      // Given
      PetCreatedEvent event1 = new PetCreatedEvent("pet-1", "Dog Pet", PetType.DOG, Instant.now());
      PetCreatedEvent event2 = new PetCreatedEvent("pet-2", "Cat Pet", PetType.CAT, Instant.now());
      PetCreatedEvent event3 = new PetCreatedEvent("pet-3", "Dragon Pet", PetType.DRAGON, Instant.now());

      // When
      projection.on(event1);
      projection.on(event2);
      projection.on(event3);

      // Then
      PetStatusView pet1 = projection.handle(new GetPetStatusQuery("pet-1"));
      PetStatusView pet2 = projection.handle(new GetPetStatusQuery("pet-2"));
      PetStatusView pet3 = projection.handle(new GetPetStatusQuery("pet-3"));

      assertThat(pet1.getName()).isEqualTo("Dog Pet");
      assertThat(pet1.getType()).isEqualTo(PetType.DOG);

      assertThat(pet2.getName()).isEqualTo("Cat Pet");
      assertThat(pet2.getType()).isEqualTo(PetType.CAT);

      assertThat(pet3.getName()).isEqualTo("Dragon Pet");
      assertThat(pet3.getType()).isEqualTo(PetType.DRAGON);
    }
  }

  @Nested
  @DisplayName("Pet Fed Event Handling")
  class PetFedEventHandling {

    @Test
    @DisplayName("should reduce hunger when pet is fed")
    void shouldReduceHungerWhenPetIsFed() {
      // Given: Create a pet first
      String petId = "pet-123";
      projection.on(new PetCreatedEvent(petId, "Hungry Pet", PetType.DOG, Instant.now()));

      // When: Feed the pet
      projection.on(new PetFedEvent(petId, 15, Instant.now()));

      // Then: Hunger should be reduced
      PetStatusView view = projection.handle(new GetPetStatusQuery(petId));
      assertThat(view.getHunger()).isEqualTo(15); // 30 - 15 = 15
    }

    @Test
    @DisplayName("should handle multiple feedings correctly")
    void shouldHandleMultipleFeedingsCorrectly() {
      // Given: Create a pet
      String petId = "pet-123";
      projection.on(new PetCreatedEvent(petId, "Very Hungry Pet", PetType.CAT, Instant.now()));

      // When: Feed multiple times
      projection.on(new PetFedEvent(petId, 10, Instant.now()));
      projection.on(new PetFedEvent(petId, 10, Instant.now()));
      projection.on(new PetFedEvent(petId, 10, Instant.now()));

      // Then: Hunger should be 0 (30 - 10 - 10 - 10 = 0)
      PetStatusView view = projection.handle(new GetPetStatusQuery(petId));
      assertThat(view.getHunger()).isEqualTo(0);
    }

    @Test
    @DisplayName("should not reduce hunger below zero")
    void shouldNotReduceHungerBelowZero() {
      // Given: Create a pet
      String petId = "pet-123";
      projection.on(new PetCreatedEvent(petId, "Overfed Pet", PetType.DRAGON, Instant.now()));

      // When: Feed with more than current hunger
      projection.on(new PetFedEvent(petId, 50, Instant.now())); // More than initial 30

      // Then: Hunger should be 0, not negative
      PetStatusView view = projection.handle(new GetPetStatusQuery(petId));
      assertThat(view.getHunger()).isEqualTo(0);
    }

    @Test
    @DisplayName("should ignore fed event for non-existent pet")
    void shouldIgnoreFedEventForNonExistentPet() {
      // When: Try to feed a pet that doesn't exist
      projection.on(new PetFedEvent("non-existent", 10, Instant.now()));

      // Then: Should not throw, just ignore
      // (This is graceful degradation - projection doesn't crash)
    }
  }

  @Nested
  @DisplayName("Query Handling")
  class QueryHandling {

    @Test
    @DisplayName("should return pet status for existing pet")
    void shouldReturnPetStatusForExistingPet() {
      // Given
      String petId = "pet-123";
      projection.on(new PetCreatedEvent(petId, "Test Pet", PetType.DOG, Instant.now()));

      // When
      PetStatusView view = projection.handle(new GetPetStatusQuery(petId));

      // Then
      assertThat(view).isNotNull();
      assertThat(view.getPetId()).isEqualTo(petId);
    }

    @Test
    @DisplayName("should throw exception for non-existent pet")
    void shouldThrowExceptionForNonExistentPet() {
      // When/Then
      assertThatThrownBy(() -> projection.handle(new GetPetStatusQuery("non-existent")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Pet not found: non-existent");
    }
  }

  @Nested
  @DisplayName("Pet Types")
  class PetTypesHandling {

    @Test
    @DisplayName("should handle DOG type correctly")
    void shouldHandleDogTypeCorrectly() {
      projection.on(new PetCreatedEvent("pet-1", "Dog", PetType.DOG, Instant.now()));
      PetStatusView view = projection.handle(new GetPetStatusQuery("pet-1"));
      assertThat(view.getType()).isEqualTo(PetType.DOG);
    }

    @Test
    @DisplayName("should handle CAT type correctly")
    void shouldHandleCatTypeCorrectly() {
      projection.on(new PetCreatedEvent("pet-2", "Cat", PetType.CAT, Instant.now()));
      PetStatusView view = projection.handle(new GetPetStatusQuery("pet-2"));
      assertThat(view.getType()).isEqualTo(PetType.CAT);
    }

    @Test
    @DisplayName("should handle DRAGON type correctly")
    void shouldHandleDragonTypeCorrectly() {
      projection.on(new PetCreatedEvent("pet-3", "Dragon", PetType.DRAGON, Instant.now()));
      PetStatusView view = projection.handle(new GetPetStatusQuery("pet-3"));
      assertThat(view.getType()).isEqualTo(PetType.DRAGON);
    }
  }

  @Nested
  @DisplayName("State Consistency")
  class StateConsistency {

    @Test
    @DisplayName("should maintain consistent state across event sequence")
    void shouldMaintainConsistentStateAcrossEventSequence() {
      // Given: A sequence of events
      String petId = "pet-123";
      projection.on(new PetCreatedEvent(petId, "Consistent Pet", PetType.CAT, Instant.now()));
      projection.on(new PetFedEvent(petId, 5, Instant.now()));
      projection.on(new PetFedEvent(petId, 5, Instant.now()));
      projection.on(new PetFedEvent(petId, 10, Instant.now()));

      // When: Query the state
      PetStatusView view = projection.handle(new GetPetStatusQuery(petId));

      // Then: State should reflect all events
      assertThat(view.getHunger()).isEqualTo(10); // 30 - 5 - 5 - 10 = 10
      assertThat(view.getName()).isEqualTo("Consistent Pet");
      assertThat(view.getType()).isEqualTo(PetType.CAT);
      assertThat(view.isAlive()).isTrue();
    }

    @Test
    @DisplayName("should preserve other stats when hunger changes")
    void shouldPreserveOtherStatsWhenHungerChanges() {
      // Given
      String petId = "pet-123";
      projection.on(new PetCreatedEvent(petId, "Preserved Pet", PetType.DRAGON, Instant.now()));

      // When: Feed the pet
      projection.on(new PetFedEvent(petId, 10, Instant.now()));

      // Then: Other stats should remain unchanged
      PetStatusView view = projection.handle(new GetPetStatusQuery(petId));
      assertThat(view.getHappiness()).isEqualTo(70); // Unchanged
      assertThat(view.getHealth()).isEqualTo(100); // Unchanged
      assertThat(view.getStage()).isEqualTo(PetStage.EGG); // Unchanged
      assertThat(view.isAlive()).isTrue(); // Unchanged
    }
  }
}
