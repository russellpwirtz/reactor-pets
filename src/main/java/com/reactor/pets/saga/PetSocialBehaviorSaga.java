package com.reactor.pets.saga;

import com.reactor.pets.command.MournPetCommand;
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
 * Saga that coordinates social interactions between pets.
 *
 * <p>Handles group-wide reactions to pet events:
 * <ul>
 *   <li>Mourning when a pet dies (current)</li>
 *   <li>Future: Celebration when a pet evolves</li>
 *   <li>Future: Jealousy when a pet receives special attention</li>
 *   <li>Future: Inspiration from achievements</li>
 *   <li>Future: Worry when pets become sick</li>
 * </ul>
 *
 * <p>This saga focuses on pet-to-pet emotional and behavioral interactions.
 * Future enhancements could incorporate:
 * <ul>
 *   <li>Friendship levels between pets</li>
 *   <li>Personality traits affecting reactions</li>
 *   <li>Proximity/grouping mechanics</li>
 *   <li>Mood contagion systems</li>
 * </ul>
 */
@Saga
@Slf4j
public class PetSocialBehaviorSaga {

  @Autowired
  private transient CommandGateway commandGateway;

  @Autowired
  private transient QueryGateway queryGateway;

  /**
   * Handle pet death - other alive pets mourn.
   *
   * <p>Mourning effect: Alive pets lose 10% of their current happiness (minimum 1).
   * This represents the emotional impact of losing a companion.
   *
   * <p>Future enhancements:
   * <ul>
   *   <li>Friendship system: Close friends mourn more (20% vs 5%)</li>
   *   <li>Grief duration: Mourning effects could persist over multiple ticks</li>
   *   <li>Comfort actions: Player could comfort mourning pets to reduce effect</li>
   *   <li>Memorial system: Creating memorials could reduce mourning intensity</li>
   * </ul>
   */
  @StartSaga
  @EndSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(PetDiedEvent event) {
    log.info("PetSocialBehaviorSaga: Pet {} died, triggering mourning for other pets",
        event.getPetId());

    try {
      // Query all alive pets (excluding the deceased)
      List<PetStatusView> alivePets = queryGateway
          .query(new GetAlivePetsQuery(),
              org.axonframework.messaging.responsetypes.ResponseTypes
                  .multipleInstancesOf(PetStatusView.class))
          .join();

      if (alivePets == null || alivePets.isEmpty()) {
        log.debug("PetSocialBehaviorSaga: No alive pets to mourn");
        return;
      }

      int mourningPetCount = 0;
      for (PetStatusView alivePet : alivePets) {
        // Skip the deceased pet itself (in case query returns it due to eventual consistency)
        if (alivePet.getPetId().equals(event.getPetId())) {
          continue;
        }

        // Calculate mourning effect: 10% happiness loss (minimum 1)
        int happinessLoss = calculateMourningEffect(alivePet);

        log.debug("PetSocialBehaviorSaga: Pet {} mourning for {} (happiness loss: {})",
            alivePet.getPetId(), event.getPetId(), happinessLoss);

        commandGateway.send(new MournPetCommand(
            alivePet.getPetId(),
            event.getPetId(),
            happinessLoss));

        mourningPetCount++;
      }

      log.info("PetSocialBehaviorSaga: {} pets mourning the death of {}",
          mourningPetCount, event.getPetId());

    } catch (Exception e) {
      log.error("PetSocialBehaviorSaga: Error processing mourning for pet {}: {}",
          event.getPetId(), e.getMessage(), e);
      // Don't propagate - mourning is a social effect, not critical to game state
    }
  }

  /**
   * Calculate the mourning effect for a pet.
   * Current implementation: 10% of current happiness (minimum 1).
   *
   * <p>Future: Could be enhanced with:
   * <ul>
   *   <li>Friendship levels (friends mourn more)</li>
   *   <li>Personality traits (some pets more empathetic)</li>
   *   <li>Recent interactions (recently played together = stronger bond)</li>
   * </ul>
   *
   * @param pet the pet experiencing mourning
   * @return happiness loss amount
   */
  private int calculateMourningEffect(PetStatusView pet) {
    // 10% of current happiness, minimum 1
    return Math.max(1, (int) Math.ceil(pet.getHappiness() * 0.1));
  }

  // Future social behavior handlers:

  // /**
  //  * Handle pet evolution - other pets get inspired.
  //  * Inspiration effect: +5 to +10 happiness depending on evolution stage.
  //  */
  // @StartSaga
  // @EndSaga
  // @SagaEventHandler(associationProperty = "petId")
  // public void on(PetEvolvedEvent event) {
  //   // Skip EGG -> BABY (not impressive)
  //   if (event.getNewStage() == PetStage.BABY) {
  //     return;
  //   }
  //
  //   List<PetStatusView> alivePets = queryAlivePetsExcluding(event.getPetId());
  //   int inspirationBonus = calculateInspirationEffect(event.getNewStage());
  //
  //   for (PetStatusView pet : alivePets) {
  //     commandGateway.send(new InspirePetCommand(
  //         pet.getPetId(),
  //         event.getPetId(),
  //         inspirationBonus));
  //   }
  // }

  // /**
  //  * Handle pet sickness - other pets become worried.
  //  * Worry effect: Slight happiness decrease, potential anxiety debuff.
  //  */
  // @StartSaga
  // @EndSaga
  // @SagaEventHandler(associationProperty = "petId")
  // public void on(PetBecameSickEvent event) {
  //   // Other pets become worried about sick companion
  // }

  // /**
  //  * Handle special attention - other pets get jealous.
  //  * Jealousy effect: Slight happiness decrease if another pet gets played with.
  //  */
  // @SagaEventHandler(associationProperty = "petId")
  // public void on(PetPlayedWithEvent event) {
  //   // Other pets might feel jealous if one gets lots of attention
  // }
}
