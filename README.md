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

## Phase 1 Complete ✅

Phase 1 provides:
- Basic Pet aggregate with event sourcing
- Create and feed pet commands
- In-memory projection for pet status
- Interactive CLI interface
- Docker-based Axon Server

## Prerequisites

- Java 21+
- Maven 3.x
- Docker & Docker Compose

## Quick Start

### 1. Start Axon Server

```bash
docker-compose up -d
```

Verify Axon Server is running:
- GUI: http://localhost:8024
- gRPC: localhost:8124

### 2. Build the Application

```bash
mvn clean package -DskipTests
```

### 3. Run the Application

**IMPORTANT**: The CLI requires an interactive terminal. Run in the foreground:

```bash
java -jar target/reactor-pets-1.0.0-SNAPSHOT.jar
```

## Developer Tooling

This project includes comprehensive code quality and formatting tools:

### Tools Configured

- **Spotless** - Automatic code formatter using Google Java Format
- **Checkstyle** - Code style checker with custom rules
- **SpotBugs** - Static analysis for bug detection
- **Maven Enforcer** - Build and dependency requirements enforcement
- **JaCoCo** - Code coverage reporting (50% minimum required)

### Common Commands

```bash
# Format all code (automatically fixes formatting issues)
mvn spotless:apply

# Check if code is properly formatted (fails if not)
mvn spotless:check

# Run code style checks
mvn checkstyle:check

# Run static analysis for bugs
mvn spotbugs:check

# Run tests and generate coverage report
mvn test
# View report at: target/site/jacoco/index.html

# Run all quality checks (automatic during build)
mvn verify

# Full clean build with all checks
mvn clean verify
```

### Pre-Commit Best Practices

Before committing code, run:

```bash
# Format code
mvn spotless:apply

# Run all checks
mvn clean verify
```

All quality checks are integrated into the Maven build lifecycle and run automatically during `mvn verify` or `mvn package`.

## Usage

### Available Commands

```
create <name> <type>  - Create a new pet (types: DOG, CAT, DRAGON)
feed <petId>          - Feed your pet (reduces hunger by 20)
status <petId>        - Display current pet status
help                  - Show this help message
exit                  - Exit the application
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

### Aggregates
- **Pet**: The write model enforcing business rules (hunger, happiness, health)

### Commands
- `CreatePetCommand(petId, name, type)`
- `FeedPetCommand(petId, foodAmount)`

### Events
- `PetCreatedEvent(petId, name, type, timestamp)`
- `PetFedEvent(petId, hungerReduction, timestamp)`

### Projections
- **PetStatusProjection**: In-memory HashMap storing current pet state

### Business Rules
- Hunger ranges from 0-100
- Cannot feed a dead pet
- Feeding reduces hunger (minimum 0)
- New pets start with: hunger=30, happiness=70, health=100, stage=EGG

## Project Structure

```
src/main/java/com/reactor/pets/
├── aggregate/
│   ├── Pet.java          # Event-sourced aggregate
│   ├── PetType.java      # Enum: DOG, CAT, DRAGON
│   └── PetStage.java     # Enum: EGG, BABY, TEEN, ADULT
├── command/
│   ├── CreatePetCommand.java
│   └── FeedPetCommand.java
├── event/
│   ├── PetCreatedEvent.java
│   └── PetFedEvent.java
├── query/
│   ├── GetPetStatusQuery.java
│   └── PetStatusView.java
├── projection/
│   └── PetStatusProjection.java  # Event handler & query handler
├── PetCliRunner.java              # CLI interface
└── ReactorPetsApplication.java    # Main Spring Boot application
```

## Viewing Events in Axon Server

1. Open http://localhost:8024
2. Navigate to "Search" → "Event Processor Browser"
3. View all events for your pet aggregates
4. Inspect event payloads and metadata

## Troubleshooting

### Axon Server Connection Issues

If you see `NOT_FOUND: [AXONIQ-1302] default: not found` warnings, these are harmless. Axon Server will auto-retry and connect successfully.

### CLI Not Responding

Ensure you're running the JAR directly in an interactive terminal, not through Maven or in the background.

## Next Steps (Phase 2+)

- [ ] Additional commands: play, clean
- [ ] Database persistence for projections
- [ ] Reactive time system with automatic stat degradation
- [ ] Pet evolution through life stages
- [ ] Multiple pets support
- [ ] Items and inventory system
- [ ] REST API for frontend integration

## Learning Outcomes

Phase 1 demonstrates:
- ✅ **Event Sourcing**: Pet state derived from events
- ✅ **CQRS**: Separate command (Pet aggregate) and query (Projection) models
- ✅ **Axon Framework**: `@Aggregate`, `@CommandHandler`, `@EventSourcingHandler`, `@QueryHandler`
- ✅ **Domain Events**: Immutable facts representing state changes
- ✅ **Projection**: Building read models from event streams

## License

Educational project demonstrating Axon Framework and Project Reactor patterns.
