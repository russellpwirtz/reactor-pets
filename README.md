# Reactor Pets - Virtual Pet (Tamagotchi)

A virtual pet application built with Axon Framework 4.x and Project Reactor, demonstrating Event Sourcing, CQRS, and reactive programming patterns.

## Technology Stack

- **Java**: 21+
- **Framework**: Spring Boot 3.2.0
- **Event Sourcing/CQRS**: Axon Framework 4.9.1
- **Reactive**: Project Reactor
- **Event Store**: Axon Server (Docker)
- **Build**: Maven
- **Code Quality**: Spotless, Checkstyle, SpotBugs, JaCoCo

## Phase 2 Complete ✅

Current features:
- Pet aggregate with event sourcing (create, feed, play, clean)
- JPA persistence with H2 database for projections
- Event history queries via EventStore
- Interactive CLI with list and history commands
- Docker-based Axon Server
- 59 passing tests with integration coverage

## Prerequisites

- Java 21+
- Maven 3.x
- Docker & Docker Compose

## Quick Start

### 1. Start Axon Server

```bash
docker-compose up -d
```

Verify Axon Server is running at http://localhost:8024 (GUI) and localhost:8124 (gRPC)

### 2. Build & Run

```bash
# Build (skip tests for faster startup)
mvn clean package -DskipTests

# Run the CLI application
mvn spring-boot:run
```

**Note:** The CLI requires an interactive terminal - run the JAR directly, not through Maven.

## Testing

```bash
# Run all tests (42 tests covering aggregates, projections, and integration)
mvn test

# Run tests with coverage report
mvn verify
# View coverage report: target/site/jacoco/index.html

# Run specific test class
mvn test -Dtest=PetAggregateTest
```

**Test Suite:** 59 tests (aggregate, projection, integration)

## Development Workflow

```bash
# Format code (before committing)
mvn spotless:apply

# Run all checks (formatting, checkstyle, spotbugs, tests)
mvn clean verify
```

**Quality Tools:** Spotless (formatter), Checkstyle (style), SpotBugs (static analysis), JaCoCo (coverage)

## Usage

### Available Commands

```
create <name> <type>     - Create a new pet (types: DOG, CAT, DRAGON)
feed <petId>             - Feed your pet (reduces hunger by 20)
play <petId>             - Play with your pet (increases happiness by 15, increases hunger by 5)
clean <petId>            - Clean your pet (increases health by 10)
status <petId>           - Display current pet status
list                     - List all pets
history <petId> [limit]  - Show event history for a pet (default 10, max 50)
help                     - Show this help message
exit                     - Exit the application
```

### Example Session

```
> create Fluffy CAT

Pet created successfully!
Pet ID: 8f3e4d2a-1b9c-4e7f-a6d5-9c8b7a6f5e4d
Name: Fluffy
Type: CAT

Use 'status 8f3e4d2a-1b9c-4e7f-a6d5-9c8b7a6f5e4d' to check your pet's status.

> status 8f3e4d2a-1b9c-4e7f-a6d5-9c8b7a6f5e4d

Pet Status:
-----------
ID: 8f3e4d2a-1b9c-4e7f-a6d5-9c8b7a6f5e4d
Name: Fluffy
Type: CAT
Stage: EGG
Status: Alive

Stats:
  Hunger: 30/100
  Happiness: 70/100
  Health: 100/100

> feed 8f3e4d2a-1b9c-4e7f-a6d5-9c8b7a6f5e4d

Pet fed successfully!
Hunger reduced by 20

Pet Status:
-----------
...
Stats:
  Hunger: 10/100
  ...
```

## Architecture

Built using **Event Sourcing** and **CQRS** patterns:

- **Aggregate:** `Pet` - write model enforcing business rules
- **Commands:** `CreatePetCommand`, `FeedPetCommand`, `PlayWithPetCommand`, `CleanPetCommand`
- **Events:** `PetCreatedEvent`, `PetFedEvent`, `PetPlayedWithEvent`, `PetCleanedEvent`
- **Projections:**
  - `PetStatusProjection` - JPA-persisted read model (H2)
  - `PetHistoryProjection` - event history from EventStore
- **Queries:** `GetPetStatusQuery`, `GetAllPetsQuery`, `GetPetHistoryQuery`

**Business Rules:** Hunger/Happiness/Health 0-100, new pets start as EGG (hunger=30, happiness=70, health=100)

See `docs/01_DESIGN.md` for detailed architecture and future phases.

## Project Structure

```
src/main/java/com/reactor/pets/
├── aggregate/
│   ├── Pet.java          # Event-sourced aggregate
│   ├── PetType.java      # Enum: DOG, CAT, DRAGON
│   └── PetStage.java     # Enum: EGG, BABY, TEEN, ADULT
├── command/
│   ├── CreatePetCommand.java
│   ├── FeedPetCommand.java
│   ├── PlayWithPetCommand.java
│   └── CleanPetCommand.java
├── event/
│   ├── PetCreatedEvent.java
│   ├── PetFedEvent.java
│   ├── PetPlayedWithEvent.java
│   └── PetCleanedEvent.java
├── query/
│   ├── GetPetStatusQuery.java
│   ├── GetAllPetsQuery.java
│   ├── GetPetHistoryQuery.java
│   ├── PetStatusView.java        # JPA entity
│   ├── PetStatusRepository.java
│   └── PetEventDto.java
├── projection/
│   ├── PetStatusProjection.java  # JPA-backed projection
│   └── PetHistoryProjection.java # EventStore-backed projection
├── PetCliRunner.java              # CLI interface
└── ReactorPetsApplication.java    # Main Spring Boot application
```

## Axon Server UI

View events and system state at http://localhost:8024:
- Navigate to "Search" → "Event Processor Browser" to view all pet events
- Inspect event payloads, metadata, and aggregate state

## Troubleshooting

- **Connection warnings:** `NOT_FOUND: [AXONIQ-1302]` warnings are harmless - Axon Server will auto-retry
- **CLI not responding:** Run the JAR directly in an interactive terminal (not through Maven or background)

## What You'll Learn

This project demonstrates:
- **Event Sourcing** - Pet state derived from immutable event stream
- **CQRS** - Separate command (write) and query (read) models
- **Axon Framework** - Aggregates, command handlers, event handlers, query handlers, EventStore
- **JPA Projections** - Persistent read models with Spring Data JPA
- **Domain-Driven Design** - Aggregates, commands, events, projections
- **Reactive Programming** - Project Reactor (coming in Phase 3)

See `docs/01_DESIGN.md` for 9 planned phases including: reactive time system, pet evolution, sagas, inventory, mini-games, and REST API.

## License

Educational project - MIT License
