package com.reactor.pets.saga;

import com.reactor.pets.command.AddItemToInventoryCommand;
import com.reactor.pets.command.MournPetCommand;
import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.query.GetAlivePetsQuery;
import com.reactor.pets.query.PetStatusView;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saga that handles pet death events.
 * Coordinates:
 * 1. Returning equipped items to inventory
 * 2. Making other alive pets mourn (lose 10% happiness)
 */
@Saga
@Slf4j
public class PetDeathSaga {

  private static final String INVENTORY_ID = "PLAYER_1_INVENTORY"; // Inventory ID

  @Autowired
  private transient CommandGateway commandGateway;

  @Autowired
  private transient QueryGateway queryGateway;

  /**
   * Handle pet death event.
   * Returns equipped items to inventory and triggers mourning for other pets.
   */
  @StartSaga
  @EndSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(PetDiedEvent event) {
    log.info("PetDeathSaga: Pet {} has died. Processing death handling...", event.getPetId());

    // Return equipped items to inventory
    if (event.getEquippedItems() != null && !event.getEquippedItems().isEmpty()) {
      log.debug("PetDeathSaga: Returning {} equipped items to inventory", event.getEquippedItems().size());
      for (EquipmentItem item : event.getEquippedItems()) {
        commandGateway.send(new AddItemToInventoryCommand(INVENTORY_ID, item));
      }
    }

    // Make other alive pets mourn (lose 10% of their happiness)
    try {
      List<PetStatusView> alivePets = queryGateway
          .query(new GetAlivePetsQuery(),
              org.axonframework.messaging.responsetypes.ResponseTypes.multipleInstancesOf(PetStatusView.class))
          .join();

      if (alivePets != null && !alivePets.isEmpty()) {
        log.debug("PetDeathSaga: {} alive pets will mourn the death of {}",
            alivePets.size(), event.getPetId());

        for (PetStatusView alivePet : alivePets) {
          // Skip the deceased pet itself (in case query returns it)
          if (alivePet.getPetId().equals(event.getPetId())) {
            continue;
          }

          // Calculate 10% happiness loss (minimum 1)
          int happinessLoss = Math.max(1, (int) Math.ceil(alivePet.getHappiness() * 0.1));

          commandGateway.send(new MournPetCommand(
              alivePet.getPetId(),
              event.getPetId(),
              happinessLoss));
        }
      }
    } catch (Exception e) {
      log.error("PetDeathSaga: Error querying alive pets for mourning", e);
    }

    log.info("PetDeathSaga: Death handling completed for pet {}", event.getPetId());
  }
}
