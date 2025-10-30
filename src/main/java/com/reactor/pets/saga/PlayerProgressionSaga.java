package com.reactor.pets.saga;

import com.reactor.pets.command.TrackPetCreationCommand;
import com.reactor.pets.event.PetCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saga that coordinates player progression tracking.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Track pet creation count (affects pet creation cost)</li>
 *   <li>Future: Track achievements</li>
 *   <li>Future: Track milestones</li>
 *   <li>Future: Handle prestige mechanics</li>
 * </ul>
 *
 * <p>This saga is separate from XPEarningSaga to maintain single responsibility.
 * XPEarningSaga handles XP earning mechanics, while this saga handles broader
 * player progression tracking.
 */
@Saga
@Slf4j
public class PlayerProgressionSaga {

  private static final String PLAYER_ID = "PLAYER_1"; // Single-player for now

  @Autowired
  private transient CommandGateway commandGateway;

  /**
   * Track when a pet is created.
   * This updates the player's total pet count, which affects:
   * <ul>
   *   <li>Pet creation cost calculation (0 XP for first, 50/100/150... for subsequent)</li>
   *   <li>Global statistics</li>
   *   <li>Future: Multi-pet license validation</li>
   *   <li>Future: Achievement triggers</li>
   * </ul>
   *
   * <p>Note: This is a single-event saga (@StartSaga + @EndSaga on same handler)
   * because we only need to react to pet creation, not maintain long-running state.
   */
  @StartSaga
  @EndSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(PetCreatedEvent event) {
    log.debug("PlayerProgressionSaga: Pet {} created for player {}, updating progression",
        event.getPetId(), PLAYER_ID);

    // Dispatch command to track pet creation in PlayerProgression aggregate
    // The aggregate will:
    // 1. Increment totalPetsCreated counter
    // 2. Apply PetCreatedForPlayerEvent
    // 3. Update event-sourced state
    commandGateway.send(new TrackPetCreationCommand(
        PLAYER_ID,
        event.getPetId(),
        event.getName(),
        event.getType()));

    log.debug("PlayerProgressionSaga: Dispatched TrackPetCreationCommand for pet {}",
        event.getPetId());
  }

  // Future handlers for progression tracking:

  // @SagaEventHandler(associationProperty = "petId")
  // public void on(PetDiedEvent event) {
  //   // Could track pet deaths for statistics
  //   // Could trigger "In Memoriam" achievement
  // }

  // @SagaEventHandler(associationProperty = "playerId")
  // public void on(XPMilestoneReachedEvent event) {
  //   // Trigger achievement unlocks
  //   // "XP Millionaire" when reaching 10,000 lifetime XP
  // }

  // @SagaEventHandler(associationProperty = "playerId")
  // public void on(PrestigeTriggeredEvent event) {
  //   // Reset progression counters
  //   // Grant prestige bonuses
  // }
}
