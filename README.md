# Reactor Pets - Virtual Pet (Tamagotchi)

A virtual pet application built with Axon Framework 4.x and Project Reactor, demonstrating Event Sourcing, CQRS, Saga pattern, and reactive programming.

## Technology Stack

- **Java**: 21+
- **Framework**: Spring Boot 3.2.0
- **Event Sourcing/CQRS**: Axon Framework 4.9.1
- **Reactive**: Project Reactor
- **Event Store**: Axon Server (Docker)
- **Build**: Maven
- **Code Quality**: Spotless, Checkstyle, SpotBugs, JaCoCo

## Phase 5 Complete ✅

**Current features:**
- Pet lifecycle with event sourcing (create, feed, play, clean)
- Reactive time system with automatic stat degradation and parallel processing
- Health deterioration and death mechanics
- Pet evolution system with saga pattern
- Evolution stages: EGG → BABY → TEEN → ADULT
- Evolution paths: HEALTHY vs NEGLECTED based on care
- ASCII art for different pet types and stages
- **Global statistics dashboard tracking all pets** (NEW)
- **Leaderboards by age, happiness, and health** (NEW)
- **PetManagerService for multi-pet management** (NEW)
- **Enhanced time tick with concurrency control** (NEW)
- JPA persistence with H2 database for projections
- Event history queries via EventStore
- Interactive CLI with dashboard and leaderboard commands
- **117 passing tests** with comprehensive coverage

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
# Build
mvn clean package -DskipTests

# Run the CLI application
mvn spring-boot:run
```

## Testing

```bash
# Run all tests (102 tests)
mvn test

# Run with coverage report
mvn verify

# View coverage: target/site/jacoco/index.html
```

## Development Workflow

```bash
# Format code
mvn spotless:apply

# Run all checks
mvn clean verify
```

## CLI Commands

```
create <name> <type>      - Create a new pet (DOG, CAT, DRAGON)
feed <petId>              - Feed your pet
play <petId>              - Play with your pet
clean <petId>             - Clean your pet
status <petId>            - Display current pet status (shows stage, stats, ASCII art)
list                      - List all alive pets
dashboard                 - Show global statistics and all alive pets
leaderboard [type]        - Show top 10 pets (AGE, HAPPINESS, HEALTH - default: AGE)
history <petId> [limit]   - Show event history
help                      - Show help
exit                      - Exit
```

## Architecture

Built with **Event Sourcing**, **CQRS**, and **Saga** patterns:

- **Aggregates:** `Pet` (write model)
- **Commands:** Create, Feed, Play, Clean, Evolve, TimeTick
- **Events:** PetCreated, PetFed, PetPlayedWith, PetCleaned, TimePassed, PetEvolved, PetDied
- **Projections:**
  - PetStatusProjection (JPA) - individual pet state
  - PetStatisticsProjection (JPA) - global statistics
  - PetHistoryProjection (EventStore) - event history
- **Saga:** PetEvolutionSaga (coordinates evolution based on age and care)
- **Reactive:** TimeTickScheduler (Flux.interval with concurrency control for batch processing)
- **Services:** PetManagerService (multi-pet management and statistics)

**Core Features:**
- Event sourcing for complete pet history
- Reactive time system for automatic stat changes
- Saga pattern for multi-step evolution process
- Evolution paths determined by care quality
- Stage-based stat degradation rates

See `docs/01_DESIGN.md` for phase roadmap and future features.

## What You'll Learn

- **Event Sourcing** - State derived from immutable event streams
- **CQRS** - Separate command and query models
- **Saga Pattern** - Coordinating long-running processes
- **Axon Framework** - Aggregates, commands, events, sagas, projections
- **Project Reactor** - Reactive programming with Flux/Mono
- **Domain-Driven Design** - Bounded contexts, aggregates, domain events

## Troubleshooting

- **Connection warnings:** `NOT_FOUND: [AXONIQ-1302]` warnings are harmless
- **CLI not responding:** Run the JAR directly in interactive terminal
- **Tests hanging:** TimeTickScheduler tests require AxonServer for proper async processing

## Next Steps

See `docs/01_DESIGN.md` for planned phases:
- **Phase 6 (Next): REST API** - Expose endpoints for web/mobile frontends
- Phase 7: Items & Inventory System
- Phase 8: Mini-Games & Achievements
- Phase 9: Advanced Features (snapshots, upcasting, deadlines)

## License

Educational project - MIT License
