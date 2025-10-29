package com.reactor.pets.saga;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.command.EvolvePetCommand;
import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.event.PetEvolvedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetHealthDeterioratedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import com.reactor.pets.event.TimePassedEvent;
import java.util.ArrayList;
import java.util.List;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Saga that manages pet evolution based on age and care quality.
 *
 * <p>Evolution criteria: - EGG → BABY: Age >= 5 (automatic) - BABY → TEEN: Age >= 20, average
 * happiness > 50 - TEEN → ADULT: Age >= 50, average health > 60, average happiness > 60
 *
 * <p>Tracks care quality as a rolling average of the last 50 ticks.
 */
@Saga
public class PetEvolutionSaga {

  private static final Logger LOG = LoggerFactory.getLogger(PetEvolutionSaga.class);
  private static final int CARE_HISTORY_SIZE = 50;

  @Autowired
  private transient CommandGateway commandGateway;

  private String petId;
  private PetStage currentStage;
  private int age;
  private int currentHealth;
  private int currentHappiness;
  private int currentHunger;
  private List<Integer> healthHistory;
  private List<Integer> happinessHistory;

  @StartSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(PetCreatedEvent event) {
    this.petId = event.getPetId();
    this.currentStage = PetStage.EGG;
    this.age = 0;
    this.currentHealth = 100;
    this.currentHappiness = 70;
    this.currentHunger = 30;
    this.healthHistory = new ArrayList<>();
    this.happinessHistory = new ArrayList<>();
    SagaLifecycle.associateWith("petId", petId);
    LOG.info("Started evolution saga for pet: {}", petId);
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(TimePassedEvent event) {
    // Update age
    this.age += event.getAgeIncrease();

    // Update stats based on time passed event
    this.currentHunger = Math.min(100, this.currentHunger + event.getHungerIncrease());
    this.currentHappiness = Math.max(0, this.currentHappiness - event.getHappinessDecrease());

    // Track current stats in history
    trackStat(healthHistory, currentHealth);
    trackStat(happinessHistory, currentHappiness);

    // Check evolution criteria
    checkEvolutionCriteria();
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(PetFedEvent event) {
    this.currentHunger = Math.max(0, this.currentHunger - event.getHungerReduction());
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(PetPlayedWithEvent event) {
    this.currentHappiness = Math.min(100, this.currentHappiness + event.getHappinessIncrease());
    this.currentHunger = Math.min(100, this.currentHunger + event.getHungerIncrease());
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(PetCleanedEvent event) {
    this.currentHealth = Math.min(100, this.currentHealth + event.getHealthIncrease());
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(PetHealthDeterioratedEvent event) {
    this.currentHealth = Math.max(0, this.currentHealth - event.getHealthDecrease());
  }

  @SagaEventHandler(associationProperty = "petId")
  public void on(PetEvolvedEvent event) {
    this.currentStage = event.getNewStage();
    LOG.info("Pet {} evolved to stage: {}", petId, currentStage);

    // Continue tracking for next evolution
  }

  @EndSaga
  @SagaEventHandler(associationProperty = "petId")
  public void on(PetDiedEvent event) {
    LOG.info("Ending evolution saga for dead pet: {}", petId);
  }

  private void checkEvolutionCriteria() {
    PetStage nextStage = null;
    EvolutionPath evolutionPath = null;
    String reason = null;

    switch (currentStage) {
      case EGG:
        if (age >= 5) {
          nextStage = PetStage.BABY;
          // Determine evolution path based on care quality
          evolutionPath = determineEvolutionPath();
          reason = "Hatched from egg at age " + age;
        }
        break;

      case BABY:
        if (age >= 20) {
          double avgHappiness = calculateAverageHappiness();
          if (avgHappiness > 50) {
            nextStage = PetStage.TEEN;
            reason = "Grew into teenager with good care (avg happiness: " + avgHappiness + ")";
          } else {
            // Still evolve, but might affect future stats
            nextStage = PetStage.TEEN;
            reason = "Grew into teenager with moderate care (avg happiness: " + avgHappiness + ")";
          }
        }
        break;

      case TEEN:
        if (age >= 50) {
          double avgHealth = calculateAverageHealth();
          double avgHappiness = calculateAverageHappiness();
          if (avgHealth > 60 && avgHappiness > 60) {
            nextStage = PetStage.ADULT;
            reason =
                String.format(
                    "Matured into adult with excellent care (health: %.1f, happiness: %.1f)",
                    avgHealth, avgHappiness);
          } else if (avgHealth > 40 || avgHappiness > 40) {
            nextStage = PetStage.ADULT;
            reason =
                String.format(
                    "Matured into adult with adequate care (health: %.1f, happiness: %.1f)",
                    avgHealth, avgHappiness);
          }
        }
        break;

      case ADULT:
        // No further evolution
        break;
      default:
        // No further evolution
        break;
    }

    if (nextStage != null) {
      // If evolutionPath not yet determined, determine it now
      if (evolutionPath == null) {
        evolutionPath = determineEvolutionPath();
      }

      LOG.info("Dispatching evolution command for pet {} to stage {}", petId, nextStage);
      commandGateway.send(new EvolvePetCommand(petId, nextStage, evolutionPath, reason));
    }
  }

  private EvolutionPath determineEvolutionPath() {
    // Determine based on care history
    double avgHealth = calculateAverageHealth();
    double avgHappiness = calculateAverageHappiness();

    // If we don't have enough history, default to HEALTHY
    if (healthHistory.isEmpty() && happinessHistory.isEmpty()) {
      return EvolutionPath.HEALTHY;
    }

    // Good care: both stats above 60
    if (avgHealth > 60 && avgHappiness > 60) {
      return EvolutionPath.HEALTHY;
    }

    // Poor care: either stat below 40
    if (avgHealth < 40 || avgHappiness < 40) {
      return EvolutionPath.NEGLECTED;
    }

    // Moderate care: default to healthy
    return EvolutionPath.HEALTHY;
  }

  private double calculateAverageHealth() {
    if (healthHistory.isEmpty()) {
      return 70.0; // Default assumption
    }
    return healthHistory.stream().mapToInt(Integer::intValue).average().orElse(70.0);
  }

  private double calculateAverageHappiness() {
    if (happinessHistory.isEmpty()) {
      return 70.0; // Default assumption
    }
    return happinessHistory.stream().mapToInt(Integer::intValue).average().orElse(70.0);
  }

  private void trackStat(List<Integer> history, int value) {
    history.add(value);
    // Keep only last CARE_HISTORY_SIZE entries
    if (history.size() > CARE_HISTORY_SIZE) {
      history.remove(0);
    }
  }
}
