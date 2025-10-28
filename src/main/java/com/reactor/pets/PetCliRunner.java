package com.reactor.pets;

import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.command.CleanPetCommand;
import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.command.FeedPetCommand;
import com.reactor.pets.command.PlayWithPetCommand;
import com.reactor.pets.query.GetAllPetsQuery;
import com.reactor.pets.query.GetPetHistoryQuery;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetEventDto;
import com.reactor.pets.query.PetStatusView;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class PetCliRunner implements CommandLineRunner {

  private final CommandGateway commandGateway;
  private final QueryGateway queryGateway;

  @Override
  public void run(String... args) {
    System.out.println("\n===========================================");
    System.out.println("   Welcome to Reactor Pets (Phase 2)");
    System.out.println("===========================================\n");

    printHelp();

    try (Scanner scanner = new Scanner(System.in)) {
      while (true) {
        System.out.print("\n> ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
          continue;
        }

        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();

        try {
          switch (command) {
            case "create":
              handleCreate(parts);
              break;
            case "feed":
              handleFeed(parts);
              break;
            case "play":
              handlePlay(parts);
              break;
            case "clean":
              handleClean(parts);
              break;
            case "status":
              handleStatus(parts);
              break;
            case "list":
              handleList();
              break;
            case "history":
              handleHistory(parts);
              break;
            case "help":
              printHelp();
              break;
            case "exit":
            case "quit":
              System.out.println("Goodbye!");
              return;
            default:
              System.out.println("Unknown command: " + command);
              System.out.println("Type 'help' for available commands.");
          }
        } catch (RuntimeException e) {
          System.out.println("Error: " + e.getMessage());
          log.error("Command execution failed", e);
        }
      }
    }
  }

  private void handleCreate(String[] parts) {
    if (parts.length < 3) {
      System.out.println("Usage: create <name> <type>");
      System.out.println("Types: DOG, CAT, DRAGON");
      return;
    }

    String name = parts[1];
    String typeStr = parts[2].toUpperCase();

    try {
      PetType type = PetType.valueOf(typeStr);
      String petId = UUID.randomUUID().toString();

      CreatePetCommand command = new CreatePetCommand(petId, name, type);
      CompletableFuture<String> result = commandGateway.send(command);

      result
          .thenAccept(
              id -> {
                System.out.println("\nPet created successfully!");
                System.out.println("Pet ID: " + id);
                System.out.println("Name: " + name);
                System.out.println("Type: " + type);
                System.out.println("\nUse 'status " + id + "' to check your pet's status.");
              })
          .exceptionally(
              ex -> {
                System.out.println("Failed to create pet: " + ex.getMessage());
                return null;
              })
          .join();

    } catch (IllegalArgumentException e) {
      System.out.println("Invalid pet type: " + typeStr);
      System.out.println("Valid types: DOG, CAT, DRAGON");
    }
  }

  private void handleFeed(String[] parts) {
    if (parts.length < 2) {
      System.out.println("Usage: feed <petId>");
      return;
    }

    String petId = parts[1];
    int foodAmount = 20; // Default feeding amount

    FeedPetCommand command = new FeedPetCommand(petId, foodAmount);
    CompletableFuture<Void> result = commandGateway.send(command);

    result
        .thenAccept(
            v -> {
              System.out.println("\nPet fed successfully!");
              System.out.println("Hunger reduced by " + foodAmount);

              // Show updated status
              try {
                Thread.sleep(100); // Brief pause to let projection update
                handleStatus(new String[] {"status", petId});
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            })
        .exceptionally(
            ex -> {
              System.out.println("Failed to feed pet: " + ex.getCause().getMessage());
              return null;
            })
        .join();
  }

  private void handleStatus(String[] parts) {
    if (parts.length < 2) {
      System.out.println("Usage: status <petId>");
      return;
    }

    String petId = parts[1];

    GetPetStatusQuery query = new GetPetStatusQuery(petId);
    CompletableFuture<PetStatusView> result = queryGateway.query(query, PetStatusView.class);

    result
        .thenAccept(
            view -> {
              System.out.println(view);
            })
        .exceptionally(
            ex -> {
              System.out.println("Failed to get pet status: " + ex.getCause().getMessage());
              return null;
            })
        .join();
  }

  private void handlePlay(String[] parts) {
    if (parts.length < 2) {
      System.out.println("Usage: play <petId>");
      return;
    }

    String petId = parts[1];

    PlayWithPetCommand command = new PlayWithPetCommand(petId);
    CompletableFuture<Void> result = commandGateway.send(command);

    result
        .thenAccept(
            v -> {
              System.out.println("\nPlayed with pet successfully!");
              System.out.println("Happiness increased by 15, Hunger increased by 5");

              // Show updated status
              try {
                Thread.sleep(100); // Brief pause to let projection update
                handleStatus(new String[] {"status", petId});
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            })
        .exceptionally(
            ex -> {
              System.out.println("Failed to play with pet: " + ex.getCause().getMessage());
              return null;
            })
        .join();
  }

  private void handleClean(String[] parts) {
    if (parts.length < 2) {
      System.out.println("Usage: clean <petId>");
      return;
    }

    String petId = parts[1];

    CleanPetCommand command = new CleanPetCommand(petId);
    CompletableFuture<Void> result = commandGateway.send(command);

    result
        .thenAccept(
            v -> {
              System.out.println("\nPet cleaned successfully!");
              System.out.println("Health increased by 10");

              // Show updated status
              try {
                Thread.sleep(100); // Brief pause to let projection update
                handleStatus(new String[] {"status", petId});
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            })
        .exceptionally(
            ex -> {
              System.out.println("Failed to clean pet: " + ex.getCause().getMessage());
              return null;
            })
        .join();
  }

  private void handleList() {
    GetAllPetsQuery query = new GetAllPetsQuery();
    CompletableFuture<List<PetStatusView>> result =
        queryGateway.query(query, ResponseTypes.multipleInstancesOf(PetStatusView.class));

    result
        .thenAccept(
            pets -> {
              if (pets.isEmpty()) {
                System.out.println("\nNo pets found. Create one with 'create <name> <type>'");
                return;
              }

              System.out.println("\n=== All Pets ===");
              System.out.println("Total pets: " + pets.size());
              System.out.println();

              for (PetStatusView pet : pets) {
                System.out.printf(
                    "%-36s | %-15s | %-8s | %-6s | H:%3d F:%3d Hp:%3d%n",
                    pet.getPetId(),
                    pet.getName(),
                    pet.getType(),
                    pet.getStage(),
                    pet.getHealth(),
                    pet.getHunger(),
                    pet.getHappiness());
              }
              System.out.println();
            })
        .exceptionally(
            ex -> {
              System.out.println("Failed to list pets: " + ex.getCause().getMessage());
              return null;
            })
        .join();
  }

  private void handleHistory(String[] parts) {
    if (parts.length < 2) {
      System.out.println("Usage: history <petId> [limit]");
      return;
    }

    String petId = parts[1];
    int limit = 10; // Default limit

    if (parts.length >= 3) {
      try {
        limit = Integer.parseInt(parts[2]);
        limit = Math.min(limit, 50); // Cap at 50
      } catch (NumberFormatException e) {
        System.out.println("Invalid limit, using default: 10");
      }
    }

    GetPetHistoryQuery query = new GetPetHistoryQuery(petId, limit);
    CompletableFuture<List<PetEventDto>> result =
        queryGateway.query(query, ResponseTypes.multipleInstancesOf(PetEventDto.class));

    result
        .thenAccept(
            events -> {
              if (events.isEmpty()) {
                System.out.println("\nNo events found for pet: " + petId);
                return;
              }

              System.out.println("\n=== Pet Event History (Last " + events.size() + ") ===");
              System.out.println();

              for (PetEventDto event : events) {
                System.out.printf(
                    "[%s] %-20s - %s%n",
                    event.getTimestamp(), event.getEventType(), event.getDetails());
              }
              System.out.println();
            })
        .exceptionally(
            ex -> {
              System.out.println("Failed to get pet history: " + ex.getCause().getMessage());
              return null;
            })
        .join();
  }

  private void printHelp() {
    System.out.println(
        """
                Available Commands:
                -------------------
                create <name> <type>     - Create a new pet (types: DOG, CAT, DRAGON)
                feed <petId>             - Feed your pet (reduces hunger by 20)
                play <petId>             - Play with your pet (increases happiness by 15, increases hunger by 5)
                clean <petId>            - Clean your pet (increases health by 10)
                status <petId>           - Display current pet status
                list                     - List all pets
                history <petId> [limit]  - Show event history for a pet (default 10, max 50)
                help                     - Show this help message
                exit                     - Exit the application

                Example:
                  > create Fluffy CAT
                  > feed <petId>
                  > play <petId>
                  > clean <petId>
                  > status <petId>
                  > list
                  > history <petId> 20
                """);
  }
}
