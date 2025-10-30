package com.reactor.pets.saga;

import com.reactor.pets.command.RemoveConsumableCommand;
import com.reactor.pets.command.UseConsumableCommand;
import com.reactor.pets.event.ConsumableUseRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saga that coordinates consumable usage across aggregates.
 * Handles:
 * 1. Validate and remove consumable from inventory
 * 2. Apply consumable effects to pet
 */
@Saga
@Slf4j
public class ConsumableUsageSaga {

  private static final String INVENTORY_ID = "PLAYER_1_INVENTORY";

  @Autowired
  private transient CommandGateway commandGateway;

  private String petId;
  private String playerId;

  /**
   * Handle consumable use request event from Pet aggregate.
   * Step 1: Remove consumable from inventory (validates quantity)
   * Step 2: Apply consumable effects to pet
   */
  @StartSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(ConsumableUseRequestedEvent event) {
    log.debug("ConsumableUsageSaga: Consumable use requested for pet {} - {}",
        event.getPetId(), event.getConsumableType());

    this.petId = event.getPetId();
    this.playerId = event.getPlayerId();

    try {
      // Step 1: Remove consumable from inventory (this validates quantity)
      log.debug("ConsumableUsageSaga: Removing {} from inventory",
          event.getConsumableType());
      commandGateway.sendAndWait(
          new RemoveConsumableCommand(INVENTORY_ID, event.getConsumableType(), 1));

      // Step 2: Apply consumable effects to pet
      log.debug("ConsumableUsageSaga: Applying {} effects to pet {}",
          event.getConsumableType(), event.getPetId());
      commandGateway.sendAndWait(
          new UseConsumableCommand(event.getPetId(), event.getConsumableType(), event.getPlayerId()));

      log.info("ConsumableUsageSaga: Successfully used {} on pet {}",
          event.getConsumableType(), event.getPetId());

    } catch (Exception e) {
      log.error("ConsumableUsageSaga: Failed to use consumable - {}", e.getMessage());
      // TODO: Compensation - add consumable back if pet command failed after inventory removal
      throw e; // Re-throw to propagate the error
    } finally {
      // Always end the saga
      SagaLifecycle.end();
    }
  }
}
