package com.reactor.pets.saga;

import com.reactor.pets.command.RemoveConsumableCommand;
import com.reactor.pets.command.UseConsumableCommand;
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
 * 1. Remove consumable from inventory
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
   * Handle consumable usage request.
   * Step 1: Remove consumable from inventory (with validation)
   * The UseConsumableCommand itself applies effects to pet
   */
  @StartSaga
  @SagaEventHandler(associationProperty = "petId")
  public void handle(UseConsumableCommand command) {
    log.debug("ConsumableUsageSaga: Starting consumable usage for pet {} - {}",
        command.getPetId(), command.getConsumableType());

    this.petId = command.getPetId();
    this.playerId = command.getPlayerId();

    try {
      // Remove consumable from inventory first (this validates quantity)
      log.debug("ConsumableUsageSaga: Removing {} from inventory",
          command.getConsumableType());
      commandGateway.sendAndWait(
          new RemoveConsumableCommand(INVENTORY_ID, command.getConsumableType(), 1));

      log.info("ConsumableUsageSaga: Successfully used {} on pet {}",
          command.getConsumableType(), command.getPetId());

      // End the saga after successful execution
      SagaLifecycle.end();
    } catch (Exception e) {
      log.error("ConsumableUsageSaga: Failed to use consumable - {}", e.getMessage());
      SagaLifecycle.end();
      throw e; // Re-throw to propagate the error
    }
  }
}
