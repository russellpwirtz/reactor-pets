package com.reactor.pets.projection;

import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetHealthDeterioratedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import com.reactor.pets.event.TimePassedEvent;
import com.reactor.pets.query.GetAlivePetsQuery;
import com.reactor.pets.query.GetAllPetsQuery;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetStatusRepository;
import com.reactor.pets.query.PetStatusView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ProcessingGroup("pet-status")
@Slf4j
@RequiredArgsConstructor
public class PetStatusProjection {

  private final PetStatusRepository petStatusRepository;

  @EventHandler
  @Transactional
  public void on(PetCreatedEvent event) {
    log.debug("Processing PetCreatedEvent for petId: {}", event.getPetId());

    PetStatusView view = new PetStatusView();
    view.setPetId(event.getPetId());
    view.setName(event.getName());
    view.setType(event.getType());
    view.setHunger(30);
    view.setHappiness(70);
    view.setHealth(100);
    view.setStage(com.reactor.pets.aggregate.PetStage.EGG);
    view.setAlive(true);
    view.setAge(0);
    view.setTotalTicks(0);
    view.setLastUpdated(event.getTimestamp());

    petStatusRepository.save(view);
    log.info("Pet created: {} ({})", event.getName(), event.getType());
  }

  @EventHandler
  @Transactional
  public void on(PetFedEvent event) {
    log.debug("Processing PetFedEvent for petId: {}", event.getPetId());

    petStatusRepository
        .findById(event.getPetId())
        .ifPresent(
            view -> {
              int newHunger = Math.max(0, view.getHunger() - event.getHungerReduction());
              view.setHunger(newHunger);
              view.setLastUpdated(event.getTimestamp());
              petStatusRepository.save(view);
              log.info(
                  "Pet {} fed. Hunger reduced by {} to {}",
                  view.getName(),
                  event.getHungerReduction(),
                  newHunger);
            });
  }

  @EventHandler
  @Transactional
  public void on(PetPlayedWithEvent event) {
    log.debug("Processing PetPlayedWithEvent for petId: {}", event.getPetId());

    petStatusRepository
        .findById(event.getPetId())
        .ifPresent(
            view -> {
              int newHappiness = Math.min(100, view.getHappiness() + event.getHappinessIncrease());
              int newHunger = Math.min(100, view.getHunger() + event.getHungerIncrease());
              view.setHappiness(newHappiness);
              view.setHunger(newHunger);
              view.setLastUpdated(event.getTimestamp());
              petStatusRepository.save(view);
              log.info(
                  "Pet {} played with. Happiness increased by {} to {}, Hunger increased by {} to {}",
                  view.getName(),
                  event.getHappinessIncrease(),
                  newHappiness,
                  event.getHungerIncrease(),
                  newHunger);
            });
  }

  @EventHandler
  @Transactional
  public void on(PetCleanedEvent event) {
    log.debug("Processing PetCleanedEvent for petId: {}", event.getPetId());

    petStatusRepository
        .findById(event.getPetId())
        .ifPresent(
            view -> {
              int newHealth = Math.min(100, view.getHealth() + event.getHealthIncrease());
              view.setHealth(newHealth);
              view.setLastUpdated(event.getTimestamp());
              petStatusRepository.save(view);
              log.info(
                  "Pet {} cleaned. Health increased by {} to {}",
                  view.getName(),
                  event.getHealthIncrease(),
                  newHealth);
            });
  }

  @QueryHandler
  public PetStatusView handle(GetPetStatusQuery query) {
    log.debug("Handling GetPetStatusQuery for petId: {}", query.getPetId());

    return petStatusRepository
        .findById(query.getPetId())
        .orElseThrow(() -> new IllegalArgumentException("Pet not found: " + query.getPetId()));
  }

  @EventHandler
  @Transactional
  public void on(TimePassedEvent event) {
    log.debug("Processing TimePassedEvent for petId: {}", event.getPetId());

    petStatusRepository
        .findById(event.getPetId())
        .ifPresent(
            view -> {
              int newHunger = Math.min(100, view.getHunger() + event.getHungerIncrease());
              int newHappiness = Math.max(0, view.getHappiness() - event.getHappinessDecrease());
              view.setHunger(newHunger);
              view.setHappiness(newHappiness);
              view.setAge(view.getAge() + event.getAgeIncrease());
              view.setTotalTicks(view.getTotalTicks() + 1);
              view.setLastUpdated(event.getTimestamp());
              petStatusRepository.save(view);
              log.debug(
                  "Time passed for pet {}. Age: {}, Ticks: {}, Hunger: {}, Happiness: {}",
                  view.getName(),
                  view.getAge(),
                  view.getTotalTicks(),
                  newHunger,
                  newHappiness);
            });
  }

  @EventHandler
  @Transactional
  public void on(PetHealthDeterioratedEvent event) {
    log.debug("Processing PetHealthDeterioratedEvent for petId: {}", event.getPetId());

    petStatusRepository
        .findById(event.getPetId())
        .ifPresent(
            view -> {
              int newHealth = Math.max(0, view.getHealth() - event.getHealthDecrease());
              view.setHealth(newHealth);
              view.setLastUpdated(event.getTimestamp());
              petStatusRepository.save(view);
              log.warn(
                  "Pet {} health deteriorated by {} to {}. Reason: {}",
                  view.getName(),
                  event.getHealthDecrease(),
                  newHealth,
                  event.getReason());
            });
  }

  @EventHandler
  @Transactional
  public void on(PetDiedEvent event) {
    log.debug("Processing PetDiedEvent for petId: {}", event.getPetId());

    petStatusRepository
        .findById(event.getPetId())
        .ifPresent(
            view -> {
              view.setAlive(false);
              view.setLastUpdated(event.getTimestamp());
              petStatusRepository.save(view);
              log.error(
                  "Pet {} has died at age {}. Total ticks: {}. Cause: {}",
                  view.getName(),
                  event.getFinalAge(),
                  event.getTotalTicks(),
                  event.getCauseOfDeath());
            });
  }

  @QueryHandler
  public List<PetStatusView> handle(GetAllPetsQuery query) {
    log.debug("Handling GetAllPetsQuery");

    return petStatusRepository.findAll();
  }

  @QueryHandler
  public List<PetStatusView> handle(GetAlivePetsQuery query) {
    log.debug("Handling GetAlivePetsQuery");

    return petStatusRepository.findByIsAlive(true);
  }
}
