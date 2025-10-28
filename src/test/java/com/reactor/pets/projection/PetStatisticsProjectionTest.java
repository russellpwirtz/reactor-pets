package com.reactor.pets.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.event.PetEvolvedEvent;
import com.reactor.pets.query.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetStatisticsProjectionTest {

  @Mock private PetStatisticsRepository statisticsRepository;

  @Mock private PetStatusRepository petStatusRepository;

  @InjectMocks private PetStatisticsProjection projection;

  private PetStatistics mockStats;

  @BeforeEach
  void setUp() {
    mockStats = new PetStatistics();
    mockStats.setId("GLOBAL");
    mockStats.setTotalPetsCreated(0);
    mockStats.setTotalPetsDied(0);
    mockStats.setLongestLivedPetAge(0);
    mockStats.setStageDistribution(new HashMap<>());
    mockStats.setLastUpdated(Instant.now());
  }

  @Test
  void shouldIncrementTotalPetsCreatedOnPetCreatedEvent() {
    // Given
    when(statisticsRepository.findById("GLOBAL")).thenReturn(Optional.of(mockStats));
    PetCreatedEvent event =
        new PetCreatedEvent("pet-1", "Fluffy", PetType.CAT, Instant.now());

    // When
    projection.on(event);

    // Then
    ArgumentCaptor<PetStatistics> captor = ArgumentCaptor.forClass(PetStatistics.class);
    verify(statisticsRepository).save(captor.capture());

    PetStatistics saved = captor.getValue();
    assertThat(saved.getTotalPetsCreated()).isEqualTo(1);
    assertThat(saved.getStageDistribution()).containsEntry(PetStage.EGG, 1);
  }

  @Test
  void shouldIncrementTotalPetsDiedOnPetDiedEvent() {
    // Given
    when(statisticsRepository.findById("GLOBAL")).thenReturn(Optional.of(mockStats));
    PetDiedEvent event = new PetDiedEvent("pet-1", 50, 500, "Hunger", Instant.now());
    PetStatusView petView = new PetStatusView();
    petView.setName("Fluffy");
    when(petStatusRepository.findById("pet-1")).thenReturn(Optional.of(petView));

    // When
    projection.on(event);

    // Then
    ArgumentCaptor<PetStatistics> captor = ArgumentCaptor.forClass(PetStatistics.class);
    verify(statisticsRepository).save(captor.capture());

    PetStatistics saved = captor.getValue();
    assertThat(saved.getTotalPetsDied()).isEqualTo(1);
  }

  @Test
  void shouldUpdateLongestLivedPetWhenNewRecordSet() {
    // Given
    mockStats.setLongestLivedPetAge(30);
    when(statisticsRepository.findById("GLOBAL")).thenReturn(Optional.of(mockStats));

    PetDiedEvent event = new PetDiedEvent("pet-2", 50, 500, "Old age", Instant.now());
    PetStatusView petView = new PetStatusView();
    petView.setName("OldTimer");
    when(petStatusRepository.findById("pet-2")).thenReturn(Optional.of(petView));

    // When
    projection.on(event);

    // Then
    ArgumentCaptor<PetStatistics> captor = ArgumentCaptor.forClass(PetStatistics.class);
    verify(statisticsRepository).save(captor.capture());

    PetStatistics saved = captor.getValue();
    assertThat(saved.getLongestLivedPetAge()).isEqualTo(50);
    assertThat(saved.getLongestLivedPetName()).isEqualTo("OldTimer");
    assertThat(saved.getLongestLivedPetId()).isEqualTo("pet-2");
  }

  @Test
  void shouldNotUpdateLongestLivedPetWhenNotARecord() {
    // Given
    mockStats.setLongestLivedPetAge(100);
    mockStats.setLongestLivedPetName("Champion");
    when(statisticsRepository.findById("GLOBAL")).thenReturn(Optional.of(mockStats));

    PetDiedEvent event = new PetDiedEvent("pet-3", 50, 500, "Hunger", Instant.now());

    // When
    projection.on(event);

    // Then
    ArgumentCaptor<PetStatistics> captor = ArgumentCaptor.forClass(PetStatistics.class);
    verify(statisticsRepository).save(captor.capture());

    PetStatistics saved = captor.getValue();
    assertThat(saved.getLongestLivedPetAge()).isEqualTo(100);
    assertThat(saved.getLongestLivedPetName()).isEqualTo("Champion");
  }

  @Test
  void shouldUpdateStageDistributionOnEvolution() {
    // Given
    Map<PetStage, Integer> distribution = new HashMap<>();
    distribution.put(PetStage.EGG, 2);
    distribution.put(PetStage.BABY, 1);
    mockStats.setStageDistribution(distribution);
    when(statisticsRepository.findById("GLOBAL")).thenReturn(Optional.of(mockStats));

    PetEvolvedEvent event =
        new PetEvolvedEvent(
            "pet-1",
            PetStage.EGG,
            PetStage.BABY,
            EvolutionPath.HEALTHY,
            "Age milestone reached",
            Instant.now());

    // When
    projection.on(event);

    // Then
    ArgumentCaptor<PetStatistics> captor = ArgumentCaptor.forClass(PetStatistics.class);
    verify(statisticsRepository).save(captor.capture());

    PetStatistics saved = captor.getValue();
    assertThat(saved.getStageDistribution().get(PetStage.EGG)).isEqualTo(1);
    assertThat(saved.getStageDistribution().get(PetStage.BABY)).isEqualTo(2);
  }

  @Test
  void shouldCreateStatisticsIfNotExists() {
    // Given
    when(statisticsRepository.findById("GLOBAL")).thenReturn(Optional.empty());
    when(statisticsRepository.save(any(PetStatistics.class))).thenAnswer(i -> i.getArgument(0));
    PetCreatedEvent event =
        new PetCreatedEvent("pet-1", "Fluffy", PetType.CAT, Instant.now());

    // When
    projection.on(event);

    // Then
    ArgumentCaptor<PetStatistics> captor = ArgumentCaptor.forClass(PetStatistics.class);
    verify(statisticsRepository).save(captor.capture());

    PetStatistics saved = captor.getValue();
    assertThat(saved.getId()).isEqualTo("GLOBAL");
    assertThat(saved.getTotalPetsCreated()).isEqualTo(1);
  }

  @Test
  void shouldHandleGetStatisticsQuery() {
    // Given
    when(statisticsRepository.findById("GLOBAL")).thenReturn(Optional.of(mockStats));

    // When
    PetStatistics result = projection.handle(new GetStatisticsQuery());

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("GLOBAL");
  }

  @Test
  void shouldHandleGetLeaderboardQueryByAge() {
    // Given
    List<PetStatusView> pets = createMockPets();
    when(petStatusRepository.findByIsAlive(true)).thenReturn(pets);

    // When
    List<PetStatusView> result =
        projection.handle(new GetLeaderboardQuery(GetLeaderboardQuery.LeaderboardType.AGE));

    // Then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getAge()).isGreaterThanOrEqualTo(result.get(1).getAge());
    assertThat(result.get(1).getAge()).isGreaterThanOrEqualTo(result.get(2).getAge());
  }

  @Test
  void shouldHandleGetLeaderboardQueryByHappiness() {
    // Given
    List<PetStatusView> pets = createMockPets();
    when(petStatusRepository.findByIsAlive(true)).thenReturn(pets);

    // When
    List<PetStatusView> result =
        projection.handle(new GetLeaderboardQuery(GetLeaderboardQuery.LeaderboardType.HAPPINESS));

    // Then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getHappiness()).isGreaterThanOrEqualTo(result.get(1).getHappiness());
    assertThat(result.get(1).getHappiness()).isGreaterThanOrEqualTo(result.get(2).getHappiness());
  }

  @Test
  void shouldLimitLeaderboardToTop10() {
    // Given
    List<PetStatusView> pets = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      PetStatusView pet = new PetStatusView();
      pet.setAge(i);
      pets.add(pet);
    }
    when(petStatusRepository.findByIsAlive(true)).thenReturn(pets);

    // When
    List<PetStatusView> result =
        projection.handle(new GetLeaderboardQuery(GetLeaderboardQuery.LeaderboardType.AGE));

    // Then
    assertThat(result).hasSize(10);
  }

  private List<PetStatusView> createMockPets() {
    PetStatusView pet1 = new PetStatusView();
    pet1.setPetId("pet-1");
    pet1.setAge(50);
    pet1.setHappiness(80);
    pet1.setHealth(90);

    PetStatusView pet2 = new PetStatusView();
    pet2.setPetId("pet-2");
    pet2.setAge(30);
    pet2.setHappiness(90);
    pet2.setHealth(70);

    PetStatusView pet3 = new PetStatusView();
    pet3.setPetId("pet-3");
    pet3.setAge(70);
    pet3.setHappiness(60);
    pet3.setHealth(85);

    return Arrays.asList(pet1, pet2, pet3);
  }
}
