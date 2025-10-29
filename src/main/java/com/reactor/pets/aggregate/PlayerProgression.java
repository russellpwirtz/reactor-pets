package com.reactor.pets.aggregate;

import com.reactor.pets.command.EarnXPCommand;
import com.reactor.pets.command.InitializePlayerCommand;
import com.reactor.pets.command.SpendXPCommand;
import com.reactor.pets.event.PetCreatedForPlayerEvent;
import com.reactor.pets.event.PlayerInitializedEvent;
import com.reactor.pets.event.XPEarnedEvent;
import com.reactor.pets.event.XPSpentEvent;
import java.time.Instant;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor
public class PlayerProgression {

  @AggregateIdentifier
  private String playerId;
  private long totalXP; // Current spendable XP
  private long lifetimeXPEarned; // Never decreases, for tracking/achievements
  private int totalPetsCreated;
  private int prestigeLevel; // Future: prestige mechanics

  @CommandHandler
  public PlayerProgression(InitializePlayerCommand command) {
    // Validate command
    if (command.getPlayerId() == null || command.getPlayerId().isBlank()) {
      throw new IllegalArgumentException("Player ID cannot be empty");
    }
    if (command.getStartingXP() < 0) {
      throw new IllegalArgumentException("Starting XP cannot be negative");
    }

    // Apply event
    AggregateLifecycle.apply(
        new PlayerInitializedEvent(
            command.getPlayerId(),
            command.getStartingXP(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(EarnXPCommand command) {
    // Business rules validation
    if (command.getXpAmount() <= 0) {
      throw new IllegalArgumentException("XP amount must be positive");
    }
    if (command.getSource() == null || command.getSource().isBlank()) {
      throw new IllegalArgumentException("XP source cannot be empty");
    }

    // Calculate new totals
    long newTotalXP = this.totalXP + command.getXpAmount();
    long newLifetimeXP = this.lifetimeXPEarned + command.getXpAmount();

    // Apply event
    AggregateLifecycle.apply(
        new XPEarnedEvent(
            command.getPlayerId(),
            command.getXpAmount(),
            newTotalXP,
            newLifetimeXP,
            command.getSource(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(SpendXPCommand command) {
    // Business rules validation
    if (command.getXpAmount() <= 0) {
      throw new IllegalArgumentException("XP amount must be positive");
    }
    if (command.getPurpose() == null || command.getPurpose().isBlank()) {
      throw new IllegalArgumentException("XP purpose cannot be empty");
    }
    if (this.totalXP < command.getXpAmount()) {
      throw new IllegalStateException(
          String.format(
              "Insufficient XP. Required: %d, Available: %d",
              command.getXpAmount(), this.totalXP));
    }

    // Calculate new total
    long newTotalXP = this.totalXP - command.getXpAmount();

    // Apply event
    AggregateLifecycle.apply(
        new XPSpentEvent(
            command.getPlayerId(),
            command.getXpAmount(),
            newTotalXP,
            command.getPurpose(),
            Instant.now()));
  }

  @EventSourcingHandler
  public void on(PlayerInitializedEvent event) {
    this.playerId = event.getPlayerId();
    this.totalXP = event.getStartingXP();
    this.lifetimeXPEarned = event.getStartingXP();
    this.totalPetsCreated = 0;
    this.prestigeLevel = 0;
  }

  @EventSourcingHandler
  public void on(XPEarnedEvent event) {
    this.totalXP = event.getNewTotalXP();
    this.lifetimeXPEarned = event.getNewLifetimeXP();
  }

  @EventSourcingHandler
  public void on(XPSpentEvent event) {
    this.totalXP = event.getNewTotalXP();
  }

  @EventSourcingHandler
  public void on(PetCreatedForPlayerEvent event) {
    this.totalPetsCreated = event.getTotalPetsCreated();
  }
}
