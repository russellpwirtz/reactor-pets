package com.reactor.pets.projection;

import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.event.PetEvolvedEvent;
import com.reactor.pets.query.GetLeaderboardQuery;
import com.reactor.pets.query.GetStatisticsQuery;
import com.reactor.pets.query.PetStatistics;
import com.reactor.pets.query.PetStatisticsRepository;
import com.reactor.pets.query.PetStatusRepository;
import com.reactor.pets.query.PetStatusView;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ProcessingGroup("pet-statistics")
@Slf4j
@RequiredArgsConstructor
public class PetStatisticsProjection {

  private static final String GLOBAL_STATS_ID = "GLOBAL";

  private final PetStatisticsRepository statisticsRepository;
  private final PetStatusRepository petStatusRepository;

  @EventHandler
  @Transactional
  public void on(PetCreatedEvent event) {
    log.debug("Updating statistics for PetCreatedEvent: {}", event.getPetId());

    PetStatistics stats = getOrCreateStatistics();
    stats.setTotalPetsCreated(stats.getTotalPetsCreated() + 1);

    // Update stage distribution
    PetStage initialStage = PetStage.EGG;
    stats
        .getStageDistribution()
        .merge(initialStage, 1, Integer::sum); // Add 1, or sum if already exists

    stats.setLastUpdated(event.getTimestamp());
    statisticsRepository.save(stats);

    log.info(
        "Statistics updated: Total pets created = {}, Stage distribution updated",
        stats.getTotalPetsCreated());
  }

  @EventHandler
  @Transactional
  public void on(PetDiedEvent event) {
    log.debug("Updating statistics for PetDiedEvent: {}", event.getPetId());

    PetStatistics stats = getOrCreateStatistics();
    stats.setTotalPetsDied(stats.getTotalPetsDied() + 1);

    // Update longest-lived pet if applicable
    if (event.getFinalAge() > stats.getLongestLivedPetAge()) {
      // Look up the pet name from the status repository
      petStatusRepository
          .findById(event.getPetId())
          .ifPresent(
              pet -> {
                stats.setLongestLivedPetId(event.getPetId());
                stats.setLongestLivedPetName(pet.getName());
                stats.setLongestLivedPetAge(event.getFinalAge());
                log.info(
                    "New longest-lived pet record: {} at age {}",
                    pet.getName(),
                    event.getFinalAge());
              });
    }

    stats.setLastUpdated(event.getTimestamp());
    statisticsRepository.save(stats);

    log.info("Statistics updated: Total pets died = {}", stats.getTotalPetsDied());
  }

  @EventHandler
  @Transactional
  public void on(PetEvolvedEvent event) {
    log.debug("Updating stage distribution for PetEvolvedEvent: {}", event.petId());

    PetStatistics stats = getOrCreateStatistics();

    // Decrement old stage count
    stats.getStageDistribution().computeIfPresent(event.oldStage(), (stage, count) -> count - 1);

    // Increment new stage count
    stats.getStageDistribution().merge(event.newStage(), 1, Integer::sum);

    // Clean up zero counts
    stats
        .getStageDistribution()
        .entrySet()
        .removeIf(entry -> entry.getValue() != null && entry.getValue() <= 0);

    stats.setLastUpdated(event.timestamp());
    statisticsRepository.save(stats);

    log.debug(
        "Stage distribution updated: {} -> {}",
        event.oldStage(),
        event.newStage());
  }

  @QueryHandler
  public PetStatistics handle(GetStatisticsQuery query) {
    log.debug("Handling GetStatisticsQuery");
    return getOrCreateStatistics();
  }

  @QueryHandler
  public List<PetStatusView> handle(GetLeaderboardQuery query) {
    log.debug("Handling GetLeaderboardQuery for type: {}", query.type());

    List<PetStatusView> allPets = petStatusRepository.findByIsAlive(true);

    return switch (query.type()) {
      case AGE -> allPets.stream()
          .sorted(Comparator.comparingInt(PetStatusView::getAge).reversed())
          .limit(10)
          .toList();
      case HAPPINESS -> allPets.stream()
          .sorted(Comparator.comparingInt(PetStatusView::getHappiness).reversed())
          .limit(10)
          .toList();
      case HEALTH -> allPets.stream()
          .sorted(Comparator.comparingInt(PetStatusView::getHealth).reversed())
          .limit(10)
          .toList();
    };
  }

  private PetStatistics getOrCreateStatistics() {
    return statisticsRepository
        .findById(GLOBAL_STATS_ID)
        .orElseGet(
            () -> {
              PetStatistics newStats = new PetStatistics();
              newStats.setId(GLOBAL_STATS_ID);
              newStats.setTotalPetsCreated(0);
              newStats.setTotalPetsDied(0);
              newStats.setLongestLivedPetAge(0);
              newStats.setLastUpdated(Instant.now());
              return newStats;
            });
  }
}
