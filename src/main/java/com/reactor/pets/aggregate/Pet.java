package com.reactor.pets.aggregate;

import com.reactor.pets.command.CleanPetCommand;
import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.command.FeedPetCommand;
import com.reactor.pets.command.PlayWithPetCommand;
import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
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
  private boolean isAlive;

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

  @EventSourcingHandler
  public void on(PetCreatedEvent event) {
    this.petId = event.getPetId();
    this.name = event.getName();
    this.type = event.getType();
    this.hunger = 30;
    this.happiness = 70;
    this.health = 100;
    this.stage = PetStage.EGG;
    this.isAlive = true;
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
}
