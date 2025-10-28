package com.reactor.pets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.query.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetManagerServiceTest {

  @Mock private QueryGateway queryGateway;

  @InjectMocks private PetManagerService petManagerService;

  private List<PetStatusView> mockAlivePets;
  private PetStatistics mockStatistics;

  @BeforeEach
  void setUp() {
    PetStatusView pet1 = new PetStatusView();
    pet1.setPetId("pet-1");
    pet1.setName("Fluffy");
    pet1.setType(PetType.CAT);
    pet1.setStage(PetStage.ADULT);
    pet1.setAge(50);
    pet1.setHappiness(80);
    pet1.setHealth(90);
    pet1.setHunger(30);

    PetStatusView pet2 = new PetStatusView();
    pet2.setPetId("pet-2");
    pet2.setName("Buddy");
    pet2.setType(PetType.DOG);
    pet2.setStage(PetStage.TEEN);
    pet2.setAge(30);
    pet2.setHappiness(90);
    pet2.setHealth(70);
    pet2.setHunger(40);

    mockAlivePets = Arrays.asList(pet1, pet2);

    mockStatistics = new PetStatistics();
    mockStatistics.setId("GLOBAL");
    mockStatistics.setTotalPetsCreated(10);
    mockStatistics.setTotalPetsDied(2);
    mockStatistics.setLongestLivedPetId("pet-old");
    mockStatistics.setLongestLivedPetName("OldTimer");
    mockStatistics.setLongestLivedPetAge(100);
    mockStatistics.setStageDistribution(new HashMap<>());
    mockStatistics.setLastUpdated(Instant.now());
  }

  @Test
  void shouldGetAlivePets() {
    // Given
    when(queryGateway.query(any(GetAlivePetsQuery.class), any(ResponseType.class)))
        .thenReturn(CompletableFuture.completedFuture(mockAlivePets));

    // When
    List<PetStatusView> result = petManagerService.getAlivePets();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Fluffy");
    assertThat(result.get(1).getName()).isEqualTo("Buddy");
    verify(queryGateway).query(any(GetAlivePetsQuery.class), any(ResponseType.class));
  }

  @Test
  void shouldGetGlobalStatistics() {
    // Given
    when(queryGateway.query(any(GetStatisticsQuery.class), eq(PetStatistics.class)))
        .thenReturn(CompletableFuture.completedFuture(mockStatistics));

    // When
    PetStatistics result = petManagerService.getGlobalStatistics();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTotalPetsCreated()).isEqualTo(10);
    assertThat(result.getTotalPetsDied()).isEqualTo(2);
    assertThat(result.getLongestLivedPetAge()).isEqualTo(100);
    verify(queryGateway).query(any(GetStatisticsQuery.class), eq(PetStatistics.class));
  }

  @Test
  void shouldGetLeaderboardByAge() {
    // Given
    when(queryGateway.query(any(GetLeaderboardQuery.class), any(ResponseType.class)))
        .thenReturn(CompletableFuture.completedFuture(mockAlivePets));

    // When
    List<PetStatusView> result =
        petManagerService.getLeaderboard(GetLeaderboardQuery.LeaderboardType.AGE);

    // Then
    assertThat(result).hasSize(2);
    verify(queryGateway).query(any(GetLeaderboardQuery.class), any(ResponseType.class));
  }

  @Test
  void shouldGetLeaderboardByHappiness() {
    // Given
    when(queryGateway.query(any(GetLeaderboardQuery.class), any(ResponseType.class)))
        .thenReturn(CompletableFuture.completedFuture(mockAlivePets));

    // When
    List<PetStatusView> result =
        petManagerService.getLeaderboard(GetLeaderboardQuery.LeaderboardType.HAPPINESS);

    // Then
    assertThat(result).hasSize(2);
    verify(queryGateway).query(any(GetLeaderboardQuery.class), any(ResponseType.class));
  }

  @Test
  void shouldGetDashboard() {
    // Given
    when(queryGateway.query(any(GetStatisticsQuery.class), eq(PetStatistics.class)))
        .thenReturn(CompletableFuture.completedFuture(mockStatistics));
    when(queryGateway.query(any(GetAlivePetsQuery.class), any(ResponseType.class)))
        .thenReturn(CompletableFuture.completedFuture(mockAlivePets));

    // When
    String dashboard = petManagerService.getDashboard();

    // Then
    assertThat(dashboard).isNotEmpty();
    assertThat(dashboard).contains("GLOBAL PET STATISTICS DASHBOARD");
    assertThat(dashboard).contains("Total Pets Created: 10");
    assertThat(dashboard).contains("Fluffy");
    assertThat(dashboard).contains("Buddy");
    assertThat(dashboard).contains("ALL ALIVE PETS");
  }

  @Test
  void shouldGetDashboardWithNoPets() {
    // Given
    when(queryGateway.query(any(GetStatisticsQuery.class), eq(PetStatistics.class)))
        .thenReturn(CompletableFuture.completedFuture(mockStatistics));
    when(queryGateway.query(any(GetAlivePetsQuery.class), any(ResponseType.class)))
        .thenReturn(CompletableFuture.completedFuture(List.of()));

    // When
    String dashboard = petManagerService.getDashboard();

    // Then
    assertThat(dashboard).isNotEmpty();
    assertThat(dashboard).contains("No alive pets");
  }

  @Test
  void shouldGetLeaderboardDisplay() {
    // Given
    when(queryGateway.query(any(GetLeaderboardQuery.class), any(ResponseType.class)))
        .thenReturn(CompletableFuture.completedFuture(mockAlivePets));

    // When
    String leaderboard =
        petManagerService.getLeaderboardDisplay(GetLeaderboardQuery.LeaderboardType.AGE);

    // Then
    assertThat(leaderboard).isNotEmpty();
    assertThat(leaderboard).contains("LEADERBOARD - TOP AGE");
    assertThat(leaderboard).contains("Fluffy");
    assertThat(leaderboard).contains("Buddy");
  }

  @Test
  void shouldDisplayHealthIndicators() {
    // Given
    PetStatusView criticalPet = new PetStatusView();
    criticalPet.setPetId("pet-critical");
    criticalPet.setName("Critical");
    criticalPet.setType(PetType.DOG);
    criticalPet.setStage(PetStage.BABY);
    criticalPet.setHealth(20); // Critical
    criticalPet.setHunger(80); // Critical
    criticalPet.setHappiness(15); // Critical

    when(queryGateway.query(any(GetStatisticsQuery.class), eq(PetStatistics.class)))
        .thenReturn(CompletableFuture.completedFuture(mockStatistics));
    when(queryGateway.query(any(GetAlivePetsQuery.class), any(ResponseType.class)))
        .thenReturn(CompletableFuture.completedFuture(List.of(criticalPet)));

    // When
    String dashboard = petManagerService.getDashboard();

    // Then
    assertThat(dashboard).contains("🔴"); // Should contain critical indicators
  }
}
