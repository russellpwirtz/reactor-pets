package com.reactor.pets.scheduler;

import com.reactor.pets.aggregate.GlobalTimeAggregate;
import com.reactor.pets.command.AdvanceGlobalTimeCommand;
import com.reactor.pets.command.CreateGlobalTimeCommand;
import com.reactor.pets.command.TimeTickCommand;
import com.reactor.pets.query.GetAlivePetsQuery;
import com.reactor.pets.query.GetGlobalTimeQuery;
import com.reactor.pets.query.GlobalTimeView;
import com.reactor.pets.query.PetStatusView;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class TimeTickScheduler {

  private final CommandGateway commandGateway;
  private final QueryGateway queryGateway;
  private Disposable subscription;

  @EventListener(ApplicationReadyEvent.class)
  public void startTimeFlow() {
    log.info("Starting reactive time flow scheduler (tick every 10 seconds)");

    // Initialize or get GlobalTimeAggregate
    initializeGlobalTime();

    subscription =
        Flux.interval(Duration.ofSeconds(10), Duration.ofSeconds(10))
            .flatMap(tick -> advanceGlobalTime())
            .flatMap(currentTick ->
                queryForAlivePets()
                    .flatMap(
                        pet -> sendTimeTick(pet, currentTick),
                        8)) // Concurrency: process up to 8 pets in parallel for better throughput
            .doOnError(
                error ->
                    log.error("Error in time tick processing: {}", error.getMessage(), error))
            .onErrorContinue(
                (error, value) ->
                    log.warn("Continuing after error for pet: {}, error: {}", value, error))
            .subscribe(
                petId -> log.debug("Time tick sent successfully to pet: {}", petId),
                error -> log.error("Fatal error in time flow: {}", error.getMessage(), error),
                () -> log.info("Time flow completed (should not happen)"));

    log.info("Time flow scheduler started successfully with concurrency control");
  }

  @PreDestroy
  public void stopTimeFlow() {
    if (subscription != null && !subscription.isDisposed()) {
      log.info("Stopping time flow scheduler");
      subscription.dispose();
    }
  }

  private Flux<PetStatusView> queryForAlivePets() {
    return Mono.fromCallable(
            () -> {
              List<PetStatusView> alivePets =
                  queryGateway
                      .query(
                          new GetAlivePetsQuery(),
                          ResponseTypes.multipleInstancesOf(PetStatusView.class))
                      .join();
              log.debug("Found {} alive pets", alivePets.size());
              return alivePets;
            })
        .flatMapIterable(pets -> pets);
  }

  private Mono<String> sendTimeTick(PetStatusView pet, long currentTick) {
    TimeTickCommand command = new TimeTickCommand(pet.getPetId(), currentTick);

    log.info(
        "*** Sending tick #{} to pet: {} (name: {}, currentGlobalTick: {})",
        currentTick,
        pet.getPetId(),
        pet.getName(),
        pet.getCurrentGlobalTick());

    return Mono.fromFuture(commandGateway.send(command))
        .thenReturn(pet.getPetId())
        .doOnSuccess(
            petId ->
                log.info(
                    "*** Time tick #{} successfully sent to pet: {} ({})",
                    currentTick, petId, pet.getName()))
        .onErrorResume(
            error -> {
              if (error.getMessage() != null
                  && error.getMessage().contains("aggregate was not found")) {
                // Pet aggregate doesn't exist in event store - log and skip
                log.warn(
                    "Skipping time tick for pet {} - aggregate not found in event store. "
                        + "This may indicate a stale projection.",
                    pet.getPetId());
                return Mono.empty(); // Continue without this pet
              }
              // For other errors, log and propagate
              log.error(
                  "Failed to send time tick #{} to pet {}: {}",
                  currentTick,
                  pet.getPetId(),
                  error.getMessage());
              return Mono.error(error);
            });
  }

  private void initializeGlobalTime() {
    try {
      log.info("*** Checking for GlobalTime aggregate...");
      GlobalTimeView globalTime =
          queryGateway
              .query(new GetGlobalTimeQuery(), ResponseTypes.instanceOf(GlobalTimeView.class))
              .join();

      if (globalTime == null) {
        log.info("*** GlobalTime aggregate not found, creating it...");
        commandGateway
            .sendAndWait(new CreateGlobalTimeCommand(GlobalTimeAggregate.GLOBAL_TIME_ID));
        log.info("*** GlobalTime aggregate created successfully");
      } else {
        log.info(
            "*** GlobalTime aggregate found at tick {}", globalTime.getCurrentGlobalTick());
      }
    } catch (Exception e) {
      log.warn("*** Failed to initialize GlobalTime, will retry on first tick", e);
    }
  }

  private Mono<Long> advanceGlobalTime() {
    return Mono.fromCallable(
            () -> {
              // Send command to advance global time
              commandGateway
                  .sendAndWait(new AdvanceGlobalTimeCommand(GlobalTimeAggregate.GLOBAL_TIME_ID));

              // Query for the new global tick value
              GlobalTimeView globalTime =
                  queryGateway
                      .query(
                          new GetGlobalTimeQuery(), ResponseTypes.instanceOf(GlobalTimeView.class))
                      .join();

              long currentTick = globalTime.getCurrentGlobalTick();
              log.info(
                  "*** Time tick #{} triggered (previous tick was #{})",
                  currentTick, currentTick - 1);
              return currentTick;
            })
        .onErrorResume(
            error -> {
              log.error("Failed to advance global time: {}", error.getMessage(), error);
              return Mono.error(error);
            });
  }
}
