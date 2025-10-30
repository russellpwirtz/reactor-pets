package com.reactor.pets.service;

import com.reactor.pets.aggregate.GlobalTimeAggregate;
import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.command.CreateGlobalTimeCommand;
import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.command.SpendXPCommand;
import com.reactor.pets.query.GetGlobalTimeQuery;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.GlobalTimeView;
import com.reactor.pets.query.PlayerProgressionView;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

/**
 * Service that handles pet creation with XP cost management.
 * Coordinates XP spending before pet creation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetCreationService {

  private static final String PLAYER_ID = "PLAYER_1";
  private static final long COST_INCREMENT = 50L;

  private final CommandGateway commandGateway;
  private final QueryGateway queryGateway;

  /**
   * Creates a new pet with XP cost management.
   * First pet is FREE, subsequent pets cost 50, 100, 150, etc.
   *
   * @param petId the ID for the new pet
   * @param name  the name for the new pet
   * @param type  the type of pet
   * @return CompletableFuture that completes when pet is created
   * @throws IllegalStateException if player has insufficient XP
   */
  public CompletableFuture<String> createPetWithCost(String petId, String name, PetType type) {
    // First, get the current global tick
    return queryGateway
        .query(new GetGlobalTimeQuery(), ResponseTypes.instanceOf(GlobalTimeView.class))
        .thenCompose(globalTime -> {
          // Ensure GlobalTime exists, create if needed
          long currentGlobalTick;
          if (globalTime == null) {
            log.info("GlobalTime not found, creating it...");
            try {
              commandGateway.sendAndWait(new CreateGlobalTimeCommand(GlobalTimeAggregate.GLOBAL_TIME_ID));
              currentGlobalTick = 0L;
            } catch (Exception e) {
              CompletableFuture<String> future = new CompletableFuture<>();
              future.completeExceptionally(new IllegalStateException("Failed to initialize GlobalTime", e));
              return future;
            }
          } else {
            currentGlobalTick = globalTime.getCurrentGlobalTick();
          }

          final long birthGlobalTick = currentGlobalTick;

          return queryGateway
              .query(new GetPlayerProgressionQuery(PLAYER_ID), PlayerProgressionView.class)
              .thenCompose(progression -> {
                int totalPetsCreated = (progression != null) ? progression.getTotalPetsCreated() : 0;

                // Calculate cost
                long xpCost = calculatePetCost(totalPetsCreated);

                log.info("Creating pet #{}: {} ({}) at global tick {} - Cost: {} XP",
                    totalPetsCreated + 1, name, type, birthGlobalTick, xpCost);

                // If cost > 0, spend XP first
                if (xpCost > 0) {
                  if (progression == null || progression.getTotalXP() < xpCost) {
                    CompletableFuture<String> future = new CompletableFuture<>();
                    future.completeExceptionally(new IllegalStateException(
                        String.format("Insufficient XP to create pet. Required: %d, Available: %d",
                            xpCost, (progression != null) ? progression.getTotalXP() : 0)));
                    return future;
                  }

                  String purpose = String.format("Create pet #%d: %s", totalPetsCreated + 1, name);

                  // Spend XP first (synchronously), then create pet
                  try {
                    commandGateway.sendAndWait(new SpendXPCommand(PLAYER_ID, xpCost, purpose));
                    return commandGateway.send(new CreatePetCommand(petId, name, type, birthGlobalTick))
                        .thenApply(result -> petId);
                  } catch (Exception e) {
                    CompletableFuture<String> future = new CompletableFuture<>();
                    future.completeExceptionally(e);
                    return future;
                  }
                } else {
                  // FREE pet, create directly
                  return commandGateway
                      .send(new CreatePetCommand(petId, name, type, birthGlobalTick))
                      .thenApply(result -> petId);
                }
              });
        });
  }

  /**
   * Calculates the XP cost for creating a pet based on how many have been
   * created.
   *
   * @param totalPetsCreated number of pets already created
   * @return XP cost (0 for first pet, 50 for second, 100 for third, etc.)
   */
  public long calculatePetCost(int totalPetsCreated) {
    if (totalPetsCreated == 0) {
      return 0L; // First pet is FREE
    }
    return totalPetsCreated * COST_INCREMENT;
  }

  /**
   * Gets the cost for the next pet without creating it.
   *
   * @return CompletableFuture with the XP cost
   */
  public CompletableFuture<Long> getNextPetCost() {
    return queryGateway
        .query(new GetPlayerProgressionQuery(PLAYER_ID), PlayerProgressionView.class)
        .thenApply(progression -> {
          int totalPetsCreated = (progression != null) ? progression.getTotalPetsCreated() : 0;
          return calculatePetCost(totalPetsCreated);
        });
  }
}
