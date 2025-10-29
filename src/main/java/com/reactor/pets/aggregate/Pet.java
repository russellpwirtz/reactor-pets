package com.reactor.pets.aggregate;

import com.reactor.pets.command.CleanPetCommand;
import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.command.EvolvePetCommand;
import com.reactor.pets.command.FeedPetCommand;
import com.reactor.pets.command.PlayWithPetCommand;
import com.reactor.pets.command.TimeTickCommand;
import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.event.PetEvolvedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetHealthDeterioratedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import com.reactor.pets.event.TimePassedEvent;
import java.time.Instant;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor
public class Pet {

  @AggregateIdentifier
  private String petId;
  private String name;
  private PetType type;
  private int hunger; // 0-100
  private int happiness; // 0-100
  private int health; // 0-100
  private PetStage stage;
  private EvolutionPath evolutionPath;
  private boolean isAlive;
  private int age; // Age in units (every 10 ticks = 1 age)
  private int totalTicks; // Total time ticks elapsed
  private long lastTickSequence; // Last processed tick sequence (for idempotency)
  private double xpMultiplier; // Multiplier for XP earned from/by this pet (starts at 1.0)

  @CommandHandler
  public Pet(CreatePetCommand command) {
    // Validate command
    if (command.getName() == null || command.getName().isBlank()) {
      throw new IllegalArgumentException("Pet name cannot be empty");
    }
    if (command.getType() == null) {
      throw new IllegalArgumentException("Pet type cannot be null");
    }

    // Apply event
    AggregateLifecycle.apply(
        new PetCreatedEvent(
            command.getPetId(), command.getName(), command.getType(), Instant.now()));
  }

  @CommandHandler
  public void handle(FeedPetCommand command) {
    // Business rules validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot feed a dead pet");
    }
    if (command.getFoodAmount() <= 0) {
      throw new IllegalArgumentException("Food amount must be positive");
    }

    // Calculate hunger reduction (cannot go below 0)
    int hungerReduction = Math.min(command.getFoodAmount(), this.hunger);

    // Apply event
    AggregateLifecycle.apply(new PetFedEvent(command.getPetId(), hungerReduction, Instant.now()));
  }

  @CommandHandler
  public void handle(PlayWithPetCommand command) {
    // Business rules validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot play with a dead pet");
    }
    if (this.happiness >= 100) {
      throw new IllegalStateException("Pet is already at maximum happiness");
    }

    // Playing increases happiness (+15) but also increases hunger (+5)
    int happinessIncrease = Math.min(15, 100 - this.happiness);
    int hungerIncrease = Math.min(5, 100 - this.hunger);

    // Apply event
    AggregateLifecycle.apply(
        new PetPlayedWithEvent(
            command.getPetId(), happinessIncrease, hungerIncrease, Instant.now()));
  }

  @CommandHandler
  public void handle(CleanPetCommand command) {
    // Business rules validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot clean a dead pet");
    }

    // Cleaning increases health (+10), capped at 100
    // Unlike playing, cleaning is allowed even at max health (it just won't increase health)
    int healthIncrease = Math.min(10, 100 - this.health);

    // Apply event
    AggregateLifecycle.apply(
        new PetCleanedEvent(command.getPetId(), healthIncrease, Instant.now()));
  }

  @CommandHandler
  public void handle(EvolvePetCommand command) {
    // Business rules validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot evolve a dead pet");
    }
    if (command.getNewStage() == null) {
      throw new IllegalArgumentException("New stage cannot be null");
    }
    if (command.getNewStage().ordinal() <= this.stage.ordinal()) {
      throw new IllegalStateException(
          "Cannot evolve to a stage lower than or equal to current stage");
    }

    // Apply event
    AggregateLifecycle.apply(
        new PetEvolvedEvent(
            command.getPetId(),
            this.stage,
            command.getNewStage(),
            command.getEvolutionPath(),
            command.getEvolutionReason(),
            Instant.now()));
  }

  @EventSourcingHandler
  public void on(PetCreatedEvent event) {
    this.petId = event.getPetId();
    this.name = event.getName();
    this.type = event.getType();
    this.hunger = 30;
    this.happiness = 70;
    this.health = 100;
    this.stage = PetStage.EGG;
    this.evolutionPath = EvolutionPath.UNDETERMINED;
    this.isAlive = true;
    this.age = 0;
    this.totalTicks = 0;
    this.lastTickSequence = -1;
    this.xpMultiplier = 1.0; // Start with 1.0x multiplier
  }

  @EventSourcingHandler
  public void on(PetFedEvent event) {
    this.hunger = Math.max(0, this.hunger - event.getHungerReduction());
  }

  @EventSourcingHandler
  public void on(PetPlayedWithEvent event) {
    this.happiness = Math.min(100, this.happiness + event.getHappinessIncrease());
    this.hunger = Math.min(100, this.hunger + event.getHungerIncrease());
  }

  @EventSourcingHandler
  public void on(PetCleanedEvent event) {
    this.health = Math.min(100, this.health + event.getHealthIncrease());
  }

  @EventSourcingHandler
  public void on(PetEvolvedEvent event) {
    this.stage = event.getNewStage();
    this.evolutionPath = event.getEvolutionPath();
  }

  @CommandHandler
  public void handle(TimeTickCommand command) {
    // Ignore if pet is dead
    if (!isAlive) {
      return; // No-op for dead pets
    }

    // Idempotency check: ignore if we've already processed this tick
    if (command.getTickCount() <= lastTickSequence) {
      return;
    }

    // Calculate stat changes based on stage and evolution path
    int baseHungerIncrease = 3;
    int baseHappinessDecrease = 2;

    // Adults have slower degradation
    if (this.stage == PetStage.ADULT) {
      baseHungerIncrease = 2;
      baseHappinessDecrease = 1;
    }

    // Neglected path has faster degradation (50% increase)
    if (this.evolutionPath == EvolutionPath.NEGLECTED) {
      baseHungerIncrease = (int) Math.ceil(baseHungerIncrease * 1.5);
      baseHappinessDecrease = (int) Math.ceil(baseHappinessDecrease * 1.5);
    }

    int hungerIncrease = Math.min(baseHungerIncrease, 100 - this.hunger);
    int happinessDecrease = Math.min(baseHappinessDecrease, this.happiness);

    // Every 10 ticks = 1 age unit
    int ageIncrease = ((totalTicks + 1) % 10 == 0) ? 1 : 0;

    // Calculate XP multiplier change
    // Increases by +0.1x every 50 ticks
    double xpMultiplierChange = 0.0;
    if ((totalTicks + 1) % 50 == 0) {
      xpMultiplierChange = 0.1;
    }

    // Check for care quality bonus: +0.05x if all stats >70 for this tick
    // (Simplified: check current stats after this tick's changes)
    int futureHunger = Math.min(100, this.hunger + hungerIncrease);
    int futureHappiness = Math.max(0, this.happiness - happinessDecrease);
    if (futureHunger <= 30 && futureHappiness >= 70 && this.health >= 70) {
      xpMultiplierChange += 0.05;
    }

    double newXpMultiplier = this.xpMultiplier + xpMultiplierChange;

    // Apply time passed event
    AggregateLifecycle.apply(
        new TimePassedEvent(
            command.getPetId(),
            hungerIncrease,
            happinessDecrease,
            ageIncrease,
            command.getTickCount(),
            xpMultiplierChange,
            newXpMultiplier,
            Instant.now()));

    // Calculate health deterioration after time has passed
    int newHunger = Math.min(100, this.hunger + hungerIncrease);
    int newHappiness = Math.max(0, this.happiness - happinessDecrease);
    int healthDecrease = 0;
    String deteriorationReason = null;

    if (newHunger > 80) {
      healthDecrease += 5;
      deteriorationReason = "Extreme hunger";
    }
    if (newHappiness < 20) {
      healthDecrease += 3;
      if (deteriorationReason != null) {
        deteriorationReason += " and low happiness";
      } else {
        deteriorationReason = "Low happiness";
      }
    }

    // Apply health deterioration if needed
    if (healthDecrease > 0) {
      healthDecrease = Math.min(healthDecrease, this.health);
      AggregateLifecycle.apply(
          new PetHealthDeterioratedEvent(
              command.getPetId(), healthDecrease, deteriorationReason, Instant.now()));
    }

    // Check for death after health deterioration
    int newHealth = this.health - healthDecrease;
    if (newHealth <= 0) {
      AggregateLifecycle.apply(
          new PetDiedEvent(
              command.getPetId(),
              this.age + ageIncrease,
              this.totalTicks + 1,
              "Health reached zero: " + deteriorationReason,
              Instant.now()));
    }
  }

  @EventSourcingHandler
  public void on(TimePassedEvent event) {
    this.hunger = Math.min(100, this.hunger + event.getHungerIncrease());
    this.happiness = Math.max(0, this.happiness - event.getHappinessDecrease());
    this.age += event.getAgeIncrease();
    this.totalTicks++;
    this.lastTickSequence = event.getTickCount();
    this.xpMultiplier = event.getNewXpMultiplier();
  }

  @EventSourcingHandler
  public void on(PetHealthDeterioratedEvent event) {
    this.health = Math.max(0, this.health - event.getHealthDecrease());
  }

  @EventSourcingHandler
  public void on(PetDiedEvent event) {
    this.isAlive = false;
  }
}
