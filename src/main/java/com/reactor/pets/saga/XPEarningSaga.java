package com.reactor.pets.saga;

import com.reactor.pets.command.EarnXPCommand;
import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetEvolvedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import com.reactor.pets.event.TimePassedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saga that coordinates XP earning across aggregates.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Track pet XP multiplier (increases with age and care quality)</li>
 *   <li>Award XP for pet interactions (feed, play, clean)</li>
 *   <li>Award XP for pet survival (per time tick)</li>
 *   <li>Award XP for pet evolution milestones</li>
 *   <li>Dispatch EarnXPCommand to PlayerProgression aggregate</li>
 * </ul>
 *
 * <p>Note: Player progression tracking (pet count, achievements, etc.) is handled
 * by {@link PlayerProgressionSaga} to maintain single responsibility.
 */
@Saga
@Slf4j
public class XPEarningSaga {

  private static final String PLAYER_ID = "PLAYER_1"; // Single-player for now

  @Autowired
  private transient CommandGateway commandGateway;

  // Track pet state for XP multiplier calculations
  private double petXpMultiplier = 1.0;

  @StartSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(PetCreatedEvent event) {
    log.debug("XPEarningSaga: Pet created - {}", event.getPetId());

    // Initialize pet XP multiplier for this pet
    this.petXpMultiplier = 1.0;

    // Note: Pet creation tracking is handled by PlayerProgressionSaga
    // Note: First pet bonus (100 XP) will be handled by initialization
    // Not awarding XP for pet creation itself in Phase 7A
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(PetFedEvent event) {
    log.debug("XPEarningSaga: Pet fed - {} (multiplier: {}x)", event.getPetId(), petXpMultiplier);

    // Feed action grants +10 XP (base) * pet's XP multiplier
    long xpEarned = (long) (10 * petXpMultiplier);

    commandGateway.send(new EarnXPCommand(
        PLAYER_ID,
        xpEarned,
        String.format("Fed pet %s", event.getPetId())));
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(PetPlayedWithEvent event) {
    log.debug("XPEarningSaga: Played with pet - {} (multiplier: {}x)", event.getPetId(), petXpMultiplier);

    // Play action grants +15 XP (base) * pet's XP multiplier
    long xpEarned = (long) (15 * petXpMultiplier);

    commandGateway.send(new EarnXPCommand(
        PLAYER_ID,
        xpEarned,
        String.format("Played with pet %s", event.getPetId())));
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(PetCleanedEvent event) {
    log.debug("XPEarningSaga: Pet cleaned - {} (multiplier: {}x)", event.getPetId(), petXpMultiplier);

    // Clean action grants +10 XP (base) * pet's XP multiplier
    long xpEarned = (long) (10 * petXpMultiplier);

    commandGateway.send(new EarnXPCommand(
        PLAYER_ID,
        xpEarned,
        String.format("Cleaned pet %s", event.getPetId())));
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(TimePassedEvent event) {
    // Update the saga's tracking of the pet's XP multiplier
    this.petXpMultiplier = event.getNewXpMultiplier();

    // Survival XP: 1 XP per tick * pet's XP multiplier
    long xpEarned = (long) (1 * petXpMultiplier);

    log.debug("XPEarningSaga: Time passed for pet {} (multiplier: {}x, XP earned: {})",
        event.getPetId(), petXpMultiplier, xpEarned);

    commandGateway.send(new EarnXPCommand(
        PLAYER_ID,
        xpEarned,
        String.format("Pet %s survived tick", event.getPetId())));
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(PetEvolvedEvent event) {
    log.debug("XPEarningSaga: Pet evolved - {} to {} (multiplier: {}x)",
        event.getPetId(), event.getNewStage(), petXpMultiplier);

    // Evolution XP bonuses:
    // EGG → BABY: 50 XP
    // BABY → TEEN: 100 XP
    // TEEN → ADULT: 200 XP
    long baseXP = switch (event.getNewStage()) {
      case BABY -> 50;
      case TEEN -> 100;
      case ADULT -> 200;
      default -> 0;
    };

    long xpEarned = (long) (baseXP * petXpMultiplier);

    commandGateway.send(new EarnXPCommand(
        PLAYER_ID,
        xpEarned,
        String.format("Pet %s evolved to %s", event.getPetId(), event.getNewStage())));
  }
}
