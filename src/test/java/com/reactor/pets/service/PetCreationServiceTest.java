package com.reactor.pets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.command.SpendXPCommand;
import com.reactor.pets.query.GetGlobalTimeQuery;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.GlobalTimeView;
import com.reactor.pets.query.PlayerProgressionView;
import java.time.Instant;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for PetCreationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PetCreationService")
class PetCreationServiceTest {

  @Mock
  private CommandGateway commandGateway;

  @Mock
  private QueryGateway queryGateway;

  @InjectMocks
  private PetCreationService petCreationService;

  private static final String PLAYER_ID = "PLAYER_1";
  private static final String PET_ID = "test-pet-123";
  private static final String PET_NAME = "Fluffy";
  private static final PetType PET_TYPE = PetType.CAT;

  private PlayerProgressionView progressionView;
  private GlobalTimeView globalTimeView;

  @BeforeEach
  void setUp() {
    progressionView = new PlayerProgressionView(
        PLAYER_ID,
        200L, // totalXP
        500L, // lifetimeXPEarned
        0, // totalPetsCreated
        0, // prestigeLevel
        Instant.now(),
        new HashSet<>());

    globalTimeView = new GlobalTimeView();
    globalTimeView.setTimeId("GLOBAL_TIME");
    globalTimeView.setCurrentGlobalTick(100L);
    globalTimeView.setLastUpdated(Instant.now());
  }

  @Test
  @DisplayName("should create first pet for free")
  void shouldCreateFirstPetForFree() {
    // Given: Player with 0 pets created
    when(queryGateway.query(any(GetGlobalTimeQuery.class), eq(ResponseTypes.instanceOf(GlobalTimeView.class))))
        .thenReturn(CompletableFuture.completedFuture(globalTimeView));
    when(queryGateway.query(any(GetPlayerProgressionQuery.class), eq(PlayerProgressionView.class)))
        .thenReturn(CompletableFuture.completedFuture(progressionView));
    when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

    // When: Creating first pet
    CompletableFuture<String> result = petCreationService.createPetWithCost(
        PET_ID, PET_NAME, PET_TYPE);

    // Then: Pet is created without spending XP
    assertThat(result).isCompletedWithValue(PET_ID);

    // Verify CreatePetCommand was sent (but no SpendXPCommand)
    ArgumentCaptor<Object> commandCaptor = ArgumentCaptor.forClass(Object.class);
    verify(commandGateway).send(commandCaptor.capture());

    assertThat(commandCaptor.getValue()).isInstanceOf(CreatePetCommand.class);
    CreatePetCommand command = (CreatePetCommand) commandCaptor.getValue();
    assertThat(command.getPetId()).isEqualTo(PET_ID);
    assertThat(command.getName()).isEqualTo(PET_NAME);
    assertThat(command.getType()).isEqualTo(PET_TYPE);
  }

  @Test
  @DisplayName("should charge 50 XP for second pet")
  void shouldCharge50XPForSecondPet() {
    // Given: Player with 1 pet created and 200 XP
    progressionView.setTotalPetsCreated(1);

    when(queryGateway.query(any(GetGlobalTimeQuery.class), eq(ResponseTypes.instanceOf(GlobalTimeView.class))))
        .thenReturn(CompletableFuture.completedFuture(globalTimeView));
    when(queryGateway.query(any(GetPlayerProgressionQuery.class), eq(PlayerProgressionView.class)))
        .thenReturn(CompletableFuture.completedFuture(progressionView));
    when(commandGateway.sendAndWait(any())).thenReturn(null);
    when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

    // When: Creating second pet
    CompletableFuture<String> result = petCreationService.createPetWithCost(
        PET_ID, PET_NAME, PET_TYPE);

    // Then: Pet is created after spending 50 XP
    assertThat(result).isCompletedWithValue(PET_ID);

    // Verify SpendXPCommand was sent with correct amount
    ArgumentCaptor<Object> commandCaptor = ArgumentCaptor.forClass(Object.class);
    verify(commandGateway).sendAndWait(commandCaptor.capture());

    assertThat(commandCaptor.getValue()).isInstanceOf(SpendXPCommand.class);
    SpendXPCommand spendCommand = (SpendXPCommand) commandCaptor.getValue();
    assertThat(spendCommand.getXpAmount()).isEqualTo(50L);
    assertThat(spendCommand.getPurpose()).contains("Create pet #2");
  }

  @Test
  @DisplayName("should charge 100 XP for third pet")
  void shouldCharge100XPForThirdPet() {
    // Given: Player with 2 pets created
    progressionView.setTotalPetsCreated(2);

    when(queryGateway.query(any(GetGlobalTimeQuery.class), eq(ResponseTypes.instanceOf(GlobalTimeView.class))))
        .thenReturn(CompletableFuture.completedFuture(globalTimeView));
    when(queryGateway.query(any(GetPlayerProgressionQuery.class), eq(PlayerProgressionView.class)))
        .thenReturn(CompletableFuture.completedFuture(progressionView));
    when(commandGateway.sendAndWait(any())).thenReturn(null);
    when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

    // When: Creating third pet
    petCreationService.createPetWithCost(PET_ID, PET_NAME, PET_TYPE);

    // Then: SpendXPCommand with 100 XP
    ArgumentCaptor<Object> commandCaptor = ArgumentCaptor.forClass(Object.class);
    verify(commandGateway).sendAndWait(commandCaptor.capture());

    SpendXPCommand spendCommand = (SpendXPCommand) commandCaptor.getValue();
    assertThat(spendCommand.getXpAmount()).isEqualTo(100L);
  }

  @Test
  @DisplayName("should charge 150 XP for fourth pet")
  void shouldCharge150XPForFourthPet() {
    // Given: Player with 3 pets created
    progressionView.setTotalPetsCreated(3);

    when(queryGateway.query(any(GetGlobalTimeQuery.class), eq(ResponseTypes.instanceOf(GlobalTimeView.class))))
        .thenReturn(CompletableFuture.completedFuture(globalTimeView));
    when(queryGateway.query(any(GetPlayerProgressionQuery.class), eq(PlayerProgressionView.class)))
        .thenReturn(CompletableFuture.completedFuture(progressionView));
    when(commandGateway.sendAndWait(any())).thenReturn(null);
    when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

    // When: Creating fourth pet
    petCreationService.createPetWithCost(PET_ID, PET_NAME, PET_TYPE);

    // Then: SpendXPCommand with 150 XP
    ArgumentCaptor<Object> commandCaptor = ArgumentCaptor.forClass(Object.class);
    verify(commandGateway).sendAndWait(commandCaptor.capture());

    SpendXPCommand spendCommand = (SpendXPCommand) commandCaptor.getValue();
    assertThat(spendCommand.getXpAmount()).isEqualTo(150L);
  }

  @Test
  @DisplayName("should fail when insufficient XP for second pet")
  void shouldFailWhenInsufficientXPForSecondPet() {
    // Given: Player with 1 pet created but only 40 XP (need 50)
    progressionView.setTotalPetsCreated(1);
    progressionView.setTotalXP(40L);

    when(queryGateway.query(any(GetGlobalTimeQuery.class), eq(ResponseTypes.instanceOf(GlobalTimeView.class))))
        .thenReturn(CompletableFuture.completedFuture(globalTimeView));
    when(queryGateway.query(any(GetPlayerProgressionQuery.class), eq(PlayerProgressionView.class)))
        .thenReturn(CompletableFuture.completedFuture(progressionView));

    // When/Then: Creating second pet fails
    CompletableFuture<String> result = petCreationService.createPetWithCost(
        PET_ID, PET_NAME, PET_TYPE);

    assertThat(result).isCompletedExceptionally();
    assertThatThrownBy(result::join)
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Insufficient XP")
        .hasMessageContaining("Required: 50")
        .hasMessageContaining("Available: 40");
  }

  @Test
  @DisplayName("should fail when insufficient XP for third pet")
  void shouldFailWhenInsufficientXPForThirdPet() {
    // Given: Player with 2 pets created but only 90 XP (need 100)
    progressionView.setTotalPetsCreated(2);
    progressionView.setTotalXP(90L);

    when(queryGateway.query(any(GetGlobalTimeQuery.class), eq(ResponseTypes.instanceOf(GlobalTimeView.class))))
        .thenReturn(CompletableFuture.completedFuture(globalTimeView));
    when(queryGateway.query(any(GetPlayerProgressionQuery.class), eq(PlayerProgressionView.class)))
        .thenReturn(CompletableFuture.completedFuture(progressionView));

    // When/Then: Creating third pet fails
    CompletableFuture<String> result = petCreationService.createPetWithCost(
        PET_ID, PET_NAME, PET_TYPE);

    assertThat(result).isCompletedExceptionally();
    assertThatThrownBy(result::join)
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Insufficient XP")
        .hasMessageContaining("Required: 100")
        .hasMessageContaining("Available: 90");
  }

  @Test
  @DisplayName("should calculate pet cost correctly")
  void shouldCalculatePetCostCorrectly() {
    assertThat(petCreationService.calculatePetCost(0)).isEqualTo(0L);
    assertThat(petCreationService.calculatePetCost(1)).isEqualTo(50L);
    assertThat(petCreationService.calculatePetCost(2)).isEqualTo(100L);
    assertThat(petCreationService.calculatePetCost(3)).isEqualTo(150L);
    assertThat(petCreationService.calculatePetCost(4)).isEqualTo(200L);
    assertThat(petCreationService.calculatePetCost(5)).isEqualTo(250L);
    assertThat(petCreationService.calculatePetCost(10)).isEqualTo(500L);
  }

  @Test
  @DisplayName("should return next pet cost")
  void shouldReturnNextPetCost() {
    // Given: Player with 2 pets created
    progressionView.setTotalPetsCreated(2);

    when(queryGateway.query(any(GetPlayerProgressionQuery.class), eq(PlayerProgressionView.class)))
        .thenReturn(CompletableFuture.completedFuture(progressionView));

    // When: Querying next pet cost
    CompletableFuture<Long> result = petCreationService.getNextPetCost();

    // Then: Cost should be 100 XP (for 3rd pet)
    assertThat(result).isCompletedWithValue(100L);
  }

  @Test
  @DisplayName("should return 0 for first pet cost")
  void shouldReturn0ForFirstPetCost() {
    // Given: Player with 0 pets created
    when(queryGateway.query(any(GetPlayerProgressionQuery.class), eq(PlayerProgressionView.class)))
        .thenReturn(CompletableFuture.completedFuture(progressionView));

    // When: Querying next pet cost
    CompletableFuture<Long> result = petCreationService.getNextPetCost();

    // Then: Cost should be 0 XP (first pet is free)
    assertThat(result).isCompletedWithValue(0L);
  }

  @Test
  @DisplayName("should handle null progression view gracefully")
  void shouldHandleNullProgressionViewGracefully() {
    // Given: No player progression exists
    when(queryGateway.query(any(GetGlobalTimeQuery.class), eq(ResponseTypes.instanceOf(GlobalTimeView.class))))
        .thenReturn(CompletableFuture.completedFuture(globalTimeView));
    when(queryGateway.query(any(GetPlayerProgressionQuery.class), eq(PlayerProgressionView.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
    when(commandGateway.send(any())).thenReturn(CompletableFuture.completedFuture(null));

    // When: Creating first pet
    CompletableFuture<String> result = petCreationService.createPetWithCost(
        PET_ID, PET_NAME, PET_TYPE);

    // Then: Pet is created for free (defaults to 0 pets created)
    assertThat(result).isCompletedWithValue(PET_ID);

    // Verify only CreatePetCommand was sent (no SpendXPCommand)
    ArgumentCaptor<Object> commandCaptor = ArgumentCaptor.forClass(Object.class);
    verify(commandGateway).send(commandCaptor.capture());
    assertThat(commandCaptor.getValue()).isInstanceOf(CreatePetCommand.class);
  }
}
