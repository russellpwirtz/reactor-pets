package com.reactor.pets.saga;

import com.reactor.pets.brain.service.PetBrainSimulator;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.event.PetEvolvedEvent;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetStatusView;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Component;

/**
 * Saga that manages pet brain simulation lifecycle.
 *
 * <p>This saga coordinates brain parameter updates based on pet state changes. It does NOT start
 * simulations automatically - simulations only start when WebSocket clients connect.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Schedule periodic parameter updates (every 5 seconds) when pet is created</li>
 *   <li>Update cached brain parameters (applied when simulation is running)</li>
 *   <li>Force immediate updates on evolution events</li>
 *   <li>Stop brain simulation and cleanup when pet dies</li>
 * </ul>
 */
@Slf4j
@Component
public class BrainLifecycleSaga {

  private final PetBrainSimulator brainSimulator;
  private final QueryGateway queryGateway;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

  // Track scheduled tasks per pet for cleanup
  private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks =
      new ConcurrentHashMap<>();

  public BrainLifecycleSaga(
      PetBrainSimulator brainSimulator,
      QueryGateway queryGateway) {
    this.brainSimulator = brainSimulator;
    this.queryGateway = queryGateway;
  }

  @EventHandler
  public void on(PetCreatedEvent event) {
    log.info("Pet created: {} - brain will initialize on first client connection", event.getPetId());

    // Schedule periodic updates every 5 seconds
    // This updates CACHED parameters even if simulation isn't running
    ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
        () -> updateBrainFromPetState(event.getPetId()),
        5, 5, TimeUnit.SECONDS
    );

    scheduledTasks.put(event.getPetId(), task);
  }

  @EventHandler
  public void on(PetEvolvedEvent event) {
    log.info("Pet {} evolved to {}, updating brain parameters",
        event.getPetId(), event.getNewStage());

    // Force immediate update on evolution
    updateBrainFromPetState(event.getPetId());
  }

  @EventHandler
  public void on(PetDiedEvent event) {
    log.info("Pet {} died, stopping brain and cleaning up", event.getPetId());

    // Stop the scheduled task
    ScheduledFuture<?> task = scheduledTasks.remove(event.getPetId());
    if (task != null) {
      task.cancel(false);
    }

    // Stop brain simulation and cleanup
    brainSimulator.stopBrain(event.getPetId());
  }

  /**
   * Updates brain parameters from current pet state.
   *
   * <p>If simulation is running, parameters are applied immediately.
   * If simulation is NOT running, parameters are cached for when it starts.
   */
  private void updateBrainFromPetState(String petId) {
    try {
      // Query current pet status
      queryGateway.query(
          new GetPetStatusQuery(petId),
          PetStatusView.class
      ).thenAccept(status -> {
        if (status != null && status.isAlive()) {
          brainSimulator.updatePetState(
              petId,
              status.getHunger(),
              status.getHappiness(),
              status.getHealth(),
              status.getStage(),
              status.getEvolutionPath()
          );
        }
      }).exceptionally(ex -> {
        log.error("Failed to update brain for pet {}", petId, ex);
        return null;
      });
    } catch (Exception e) {
      log.error("Error updating brain for pet {}", petId, e);
    }
  }
}
