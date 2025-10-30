package com.reactor.pets.aggregate;

import com.reactor.pets.command.AdvanceGlobalTimeCommand;
import com.reactor.pets.command.CreateGlobalTimeCommand;
import com.reactor.pets.event.GlobalTimeAdvancedEvent;
import com.reactor.pets.event.GlobalTimeCreatedEvent;
import java.time.Instant;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor
public class GlobalTimeAggregate {

  public static final String GLOBAL_TIME_ID = "GLOBAL_TIME";

  @AggregateIdentifier
  private String timeId;
  private long currentGlobalTick;

  @CommandHandler
  public GlobalTimeAggregate(CreateGlobalTimeCommand command) {
    // Validate that we're using the singleton ID
    if (!GLOBAL_TIME_ID.equals(command.getTimeId())) {
      throw new IllegalArgumentException("GlobalTime must use ID: " + GLOBAL_TIME_ID);
    }

    // Apply event
    AggregateLifecycle.apply(
        new GlobalTimeCreatedEvent(command.getTimeId(), Instant.now()));
  }

  @CommandHandler
  public void handle(AdvanceGlobalTimeCommand command) {
    // Validate aggregate is initialized
    if (timeId == null) {
      throw new IllegalStateException("GlobalTime aggregate not initialized");
    }

    // Apply event - tick will be incremented
    AggregateLifecycle.apply(
        new GlobalTimeAdvancedEvent(
            command.getTimeId(),
            currentGlobalTick + 1,
            Instant.now()));
  }

  @EventSourcingHandler
  public void on(GlobalTimeCreatedEvent event) {
    this.timeId = event.getTimeId();
    this.currentGlobalTick = 0; // Start at tick 0
  }

  @EventSourcingHandler
  public void on(GlobalTimeAdvancedEvent event) {
    this.currentGlobalTick = event.getNewGlobalTick();
  }
}
