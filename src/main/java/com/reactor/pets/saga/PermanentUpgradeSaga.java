package com.reactor.pets.saga;

import com.reactor.pets.command.ApplyPermanentModifierCommand;
import com.reactor.pets.domain.EquipmentCatalog;
import com.reactor.pets.domain.PermanentUpgrade;
import com.reactor.pets.domain.UpgradeType;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.UpgradePurchasedEvent;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.PetStatusView;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saga that coordinates the application of permanent upgrade modifiers to pets.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>When a permanent upgrade is purchased: apply it to all existing alive pets</li>
 *   <li>When a new pet is created: apply all currently owned permanent upgrades to it</li>
 * </ul>
 *
 * <p>This ensures that:
 * <ul>
 *   <li>Existing pets benefit from newly purchased upgrades immediately</li>
 *   <li>New pets automatically receive all previously purchased upgrades</li>
 *   <li>The Pet aggregate remains event-sourced and self-contained</li>
 * </ul>
 */
@Saga
@Slf4j
public class PermanentUpgradeSaga {

  private static final String PLAYER_ID = "PLAYER_1"; // Single-player for now

  @Autowired
  private transient CommandGateway commandGateway;

  @Autowired
  private transient QueryGateway queryGateway;

  /**
   * Handle permanent upgrade purchased event.
   * Apply the new upgrade to all existing alive pets.
   */
  @StartSaga
  @EndSaga
  @SagaEventHandler(associationProperty = "playerId")
  public void on(UpgradePurchasedEvent event) {
    log.debug("PermanentUpgradeSaga: Upgrade purchased: {}, applying to all pets",
        event.getUpgradeType());

    try {
      // Create the permanent upgrade from catalog
      PermanentUpgrade upgrade = EquipmentCatalog.createUpgrade(event.getUpgradeType());

      // Query all alive pets
      List<PetStatusView> alivePets = queryGateway.query(
          new com.reactor.pets.query.GetAlivePetsQuery(),
          ResponseTypes.multipleInstancesOf(PetStatusView.class)
      ).join();

      log.info("PermanentUpgradeSaga: Applying {} to {} alive pets",
          event.getUpgradeType(), alivePets.size());

      // Apply upgrade to each alive pet
      for (PetStatusView pet : alivePets) {
        commandGateway.send(new ApplyPermanentModifierCommand(pet.getPetId(), upgrade));
        log.debug("PermanentUpgradeSaga: Applied {} to pet {}", event.getUpgradeType(), pet.getPetId());
      }

      log.info("PermanentUpgradeSaga: Successfully applied {} to all {} pets",
          event.getUpgradeType(), alivePets.size());

    } catch (Exception e) {
      log.error("PermanentUpgradeSaga: Failed to apply upgrade to pets", e);
    }
  }

  /**
   * Handle pet created event.
   * Apply all currently owned permanent upgrades to the new pet.
   */
  @StartSaga
  @EndSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(PetCreatedEvent event) {
    log.debug("PermanentUpgradeSaga: Pet {} created, applying existing permanent upgrades",
        event.getPetId());

    try {
      // Query player progression to get owned upgrades
      var progression = queryGateway.query(
          new GetPlayerProgressionQuery(PLAYER_ID),
          ResponseTypes.instanceOf(com.reactor.pets.query.PlayerProgressionView.class)
      ).join();

      if (progression == null || progression.getPermanentUpgrades() == null) {
        log.debug("PermanentUpgradeSaga: No permanent upgrades to apply to new pet {}",
            event.getPetId());
        return;
      }

      Set<UpgradeType> ownedUpgrades = progression.getPermanentUpgrades();
      log.info("PermanentUpgradeSaga: Applying {} permanent upgrades to new pet {}",
          ownedUpgrades.size(), event.getPetId());

      // Apply each owned upgrade to the new pet
      for (UpgradeType upgradeType : ownedUpgrades) {
        PermanentUpgrade upgrade = EquipmentCatalog.createUpgrade(upgradeType);
        commandGateway.send(new ApplyPermanentModifierCommand(event.getPetId(), upgrade));
        log.debug("PermanentUpgradeSaga: Applied {} to new pet {}",
            upgradeType, event.getPetId());
      }

      log.info("PermanentUpgradeSaga: Successfully applied all upgrades to new pet {}",
          event.getPetId());

    } catch (Exception e) {
      log.error("PermanentUpgradeSaga: Failed to apply upgrades to new pet {}", event.getPetId(), e);
    }
  }
}
