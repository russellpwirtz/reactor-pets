# Virtual Pet (Tamagotchi) - Axon Framework & Project Reactor
## Design Document

### Technology Stack
- **Language:** Java 21+
- **Framework:** Spring Boot 3.x
- **Event Sourcing/CQRS:** Axon Framework 4.x
- **Reactive:** Project Reactor (reactor-core)
- **Event Store:** Axon Server (via Docker)
- **Build Tool:** Maven
- **Interface:** CLI + REST API (Swagger docs available at `/swagger-ui.html`)

---

## Architecture Overview

### Core Concepts
- **Aggregate:** `Pet` - the write model, enforces business rules
- **Commands:** User intentions (CreatePet, FeedPet, PlayWithPet, etc.)
- **Events:** Facts that happened (PetCreated, PetFed, PetBecameHungry, etc.)
- **Projections:** Read models for queries (current pet status, statistics)
- **Sagas:** Multi-step workflows (health management, evolution triggers)
- **Reactive Time:** Project Reactor handles scheduled degradation

### Bounded Contexts (Initial)
1. **Pet Lifecycle** - core pet state and basic needs
2. **Time Management** - handles periodic ticks and degradation

---

## Current Project Status

**Active Phase:** Phase 6 Complete - Ready for Phase 7

**Build Status:** ✅ All checks passing (Checkstyle, SpotBugs, tests)
**Test Suite:** 117 tests passing
**REST API:** ✅ Fully operational with 9 endpoints + Swagger documentation

**Available Commands:**
- `create <name> <type>` - Create new pet (DOG, CAT, or DRAGON)
- `feed <petId>` - Feed pet to reduce hunger (-20)
- `play <petId>` - Play with pet (happiness +15, hunger +5)
- `clean <petId>` - Clean pet (health +10)
- `status <petId>` - View current pet status
- `list` - Show all pets
- `dashboard` - Show global statistics and all alive pets
- `leaderboard [type]` - Show top 10 pets (AGE, HAPPINESS, HEALTH)
- `history <petId> [limit]` - Show event history (default 10, max 50)
- `help` - Display command reference
- `exit` - Terminate application

**Next Steps:** Proceed with Phase 7 - Items System & Inventory Bounded Context

---

## Phase 1: Foundation & Basic Pet Lifecycle ✅ COMPLETED

**Goal:** Runnable project with Axon Server, basic Pet aggregate, and ability to create/feed a pet via CLI.

### Completed Features
- Pet Aggregate with event sourcing (PetCreatedEvent, PetFedEvent)
- CQRS with CreatePetCommand, FeedPetCommand, and GetPetStatusQuery
- In-memory projection for pet status
- Interactive CLI (create, feed, status commands)
- Docker setup with Axon Server
- Developer tooling (Spotless, Checkstyle, SpotBugs, JaCoCo)
- Comprehensive test suite

---

## Phase 2: Multiple Interactions & State Projections ✅ COMPLETED

**Goal:** Add more interactions (play, clean) and persist projections to database.

### Completed Features
- Play and clean interactions (PlayWithPetCommand, CleanPetCommand)
- New events: PetPlayedWithEvent, PetCleanedEvent
- JPA persistence with H2 database for projections
- PetHistoryProjection reading from EventStore
- GetAllPetsQuery and GetPetHistoryQuery
- CLI commands: play, clean, list, history

---

## Phase 3: Reactive Time System & Degradation ✅ COMPLETED

**Goal:** Implement Project Reactor for time-based stat degradation (hunger increases, happiness decreases over time).

### Completed Features
- TimeTickScheduler using Flux.interval for automatic 10-second ticks
- TimeTickCommand and TimePassedEvent for stat degradation
- Automatic hunger increase (+3) and happiness decrease (-2) per tick
- Age tracking (increments every 10 ticks)
- Health deterioration when hunger > 80 or happiness < 20
- Death mechanic when health reaches 0 (PetDiedEvent)
- Business rules prevent commands on dead pets
- GetAlivePetsQuery for filtering active pets
- Visual indicators in CLI (🔴/🟡 for critical stats)
- Idempotent time tick processing with sequence numbers

---

## Phase 4: Evolution System & Pet Stages ✅ COMPLETED

**Goal:** Pets evolve through stages based on age and care quality, implementing Saga pattern.

### Completed Features
- PetEvolutionSaga tracks pet care quality and triggers evolution at age milestones
- Evolution stages: EGG → BABY → TEEN → ADULT
- Evolution paths: HEALTHY (good care) vs NEGLECTED (poor care)
- Evolution criteria:
  - **EGG → BABY:** Age >= 5 (automatic)
  - **BABY → TEEN:** Age >= 20 with happiness check
  - **TEEN → ADULT:** Age >= 50 with health/happiness checks
- Stage-based stat degradation (adults degrade slower, neglected path degrades faster)
- EvolvePetCommand and PetEvolvedEvent for evolution handling
- ASCII art for different pet types and stages displayed in CLI
- Status command shows current stage, evolution path, and ASCII art

---

## Phase 5: Multiple Pets & Statistics Dashboard ✅ COMPLETED

**Goal:** Support multiple concurrent pets and aggregate statistics across all pets.

### Completed Features
1. **Pet Manager Service** ✅
   - Created `PetManagerService` with methods for managing multiple pets
   - Query all active pets
   - Access global statistics
   - Generate formatted dashboard and leaderboard displays

2. **New Projections** ✅
   - **PetStatisticsProjection:** Tracks global statistics
     - Total pets created
     - Total pets died
     - Average lifespan (simplified calculation)
     - Longest-lived pet (name, ID, age)
     - Evolution stage distribution by stage
   - Listens to PetCreatedEvent, PetDiedEvent, PetEvolvedEvent
   - Single-row entity with ID "GLOBAL"

3. **Enhanced Time Tick** ✅
   - Batch processing with `Flux.interval` and `flatMap` with concurrency of 8
   - Error handling: `onErrorContinue` ensures one pet failure doesn't affect others
   - Query alive pets and send tick to each in parallel
   - Improved logging for debugging

4. **CLI Dashboard** ✅
   - `dashboard` command shows global stats + all alive pets
   - `leaderboard [type]` shows top 10 pets sorted by AGE, HAPPINESS, or HEALTH
   - Color-coded health indicators (🔴 🟡) for critical stats
   - Trophy emojis (🏆 🥈 🥉) for leaderboard rankings

5. **Event Handler Optimizations** ✅
   - `@ProcessingGroup("pet-statistics")` on PetStatisticsProjection
   - Configured tracking processors in `application.yml` with batch sizes
   - Separate processing groups for pet-status and pet-statistics

### Technical Implementation
- Multiple pet aggregates managed by Axon's repository with unique UUIDs
- PetStatisticsProjection maintains single global statistics record
- All projections listen to pet events across aggregates
- Query handlers for GetStatisticsQuery and GetLeaderboardQuery
- Comprehensive test suite with 15 new tests for Phase 5 features

---

## Phase 6: REST API & JSON Interface ✅ COMPLETED

**Goal:** Expose REST API for all operations, preparing for Next.js frontend integration.

**Note:** This phase was moved up in priority (originally Phase 8) to enable web/mobile frontend development before implementing game-specific features.

### Completed Features

1. **REST Controllers** ✅
   - `PetController` with 7 endpoints for pet operations
   - `StatisticsController` with 2 endpoints for stats and leaderboards
   - Async command handling with `CompletableFuture`
   - Proper HTTP status codes and error responses

2. **Endpoints** ✅
   - `POST /api/pets` - Create new pet
   - `GET /api/pets` - List all pets
   - `GET /api/pets/{id}` - Get pet status
   - `POST /api/pets/{id}/feed` - Feed pet
   - `POST /api/pets/{id}/play` - Play with pet
   - `POST /api/pets/{id}/clean` - Clean pet
   - `GET /api/pets/{id}/history` - Get event history
   - `GET /api/statistics` - Global statistics
   - `GET /api/leaderboard?type={AGE|HAPPINESS|HEALTH}` - Leaderboard

3. **DTOs & Serialization** ✅
   - `CreatePetRequest` with validation
   - `PetStatusResponse` with ASCII art
   - `StatisticsResponse` for global stats
   - `LeaderboardResponse` with top 10 pets
   - `PetHistoryResponse` for event timeline
   - `ErrorResponse` for standardized errors

4. **Error Handling** ✅
   - `GlobalExceptionHandler` for consistent error responses
   - Handles Axon exceptions (CommandExecutionException, QueryExecutionException)
   - Validation errors with field-level details
   - Generic exception fallback

5. **CORS Configuration** ✅
   - Allows localhost:3000 (Next.js)
   - Allows localhost:5173 (Vite)
   - Allows localhost:8080 (same origin)
   - Configured for all HTTP methods

6. **API Documentation** ✅
   - Springdoc OpenAPI integration
   - Swagger UI at `/swagger-ui.html`
   - Interactive API testing interface
   - Schema definitions for all DTOs

7. **XStream Serialization Fixes** ✅
   - Converted Java records to Lombok classes for Axon compatibility
   - Fixed: `EvolvePetCommand`, `PetEvolvedEvent`, `GetLeaderboardQuery`, `GetStatisticsQuery`
   - Updated accessor methods from `field()` to `getField()` syntax

---

## Phase 7: Items System & Inventory Bounded Context

**Goal:** Introduce food types, toys, and medicine as separate bounded context with inventory management.

**Note:** This phase was originally Phase 6 but moved after REST API implementation.

### Deliverables
1. **Inventory Aggregate**
   ```java
   @Aggregate
   class Inventory {
     @AggregateIdentifier
     private String inventoryId; // One per player/user
     private Map<ItemType, Integer> items; // APPLE:5, BALL:2, etc.
     
     // Commands: AddItemCommand, UseItemCommand
     // Events: ItemAddedEvent, ItemUsedEvent, ItemDepletedEvent
   }
   ```

2. **Item Types**
   - **Food:** Apple (hunger -15), Pizza (hunger -25, happiness +5), Medicine (health +20)
   - **Toys:** Ball (happiness +10), Robot (happiness +15, play requirement)
   - Starting inventory: 10 apples, 5 balls, 2 medicine

3. **Cross-Aggregate Commands**
   - `FeedPetWithItemCommand(petId, inventoryId, itemType)`
   - Inventory Saga coordinates:
     1. Check inventory has item (Query)
     2. Dispatch UseItemCommand to Inventory
     3. Dispatch FeedPetCommand/PlayWithPetCommand/GiveMedicineCommand to Pet

4. **New Pet Command**
   - `GiveMedicineCommand(petId, medicineType)` → `PetMedicatedEvent(petId, healthIncrease, sicknessCured)`

5. **Sickness Mechanic**
   - If health < 30, chance of `PetBecameSickEvent` (reduces all stats faster)
   - Medicine cures sickness, restores health
   - Cannot play when sick (business rule)

6. **CLI Updates**
   - `inventory` - show current items
   - `feed <petId> <itemType>` - feed specific food
   - `use <petId> <itemType>` - use toy/medicine
   - `give <itemType> <quantity>` - cheat command to add items for testing

### Technical Notes
- Saga pattern for cross-aggregate coordination
- Use `@SagaEventHandler` with multiple associations (petId, inventoryId)
- Query handlers to check inventory before item use
- Event-driven: ItemUsedEvent triggers pet action

---

## Phase 8: Mini-Games & Achievement System

**Goal:** Add interactive mini-games and track achievements across pet lifetime.

### Deliverables
1. **Mini-Game Commands**
   - `PlayGuessGameCommand(petId, playerGuess)` → success/fail affects happiness
   - `PlayReflexGameCommand(petId, reactionTime)` → fast reactions increase happiness more
   - Games consume happiness to play (cost: -5 happiness)

2. **Achievement Aggregate**
   ```java
   @Aggregate
   class PlayerAchievements {
     @AggregateIdentifier
     private String playerId;
     private Set<Achievement> unlockedAchievements;
     
     // Achievements: FirstPet, VeteranOwner (10 pets), PerfectCare (adult with 90+ all stats), etc.
   }
   ```

3. **Achievement Tracking Saga**
   - Listens to pet events and inventory events
   - Checks achievement criteria
   - Dispatches `UnlockAchievementCommand` when earned

4. **Achievement Types**
   - **First Pet:** Create first pet
   - **Veteran Owner:** Create 10 pets (dead or alive)
   - **Perfect Care:** Evolve pet to adult with all stats > 90
   - **Survivor:** Keep pet alive for 500 ticks
   - **Collector:** Own 10+ items in inventory
   - **Healer:** Use medicine 5 times

5. **Mini-Game Logic**
   - Guess game: random number 1-5, player guesses
   - Reflex game: CLI shows "GO!" with timestamp, player hits enter, calculate reaction time
   - Success increases happiness more (+20 vs normal play +15)

6. **CLI Features**
   - `game guess <petId>` - start guess game
   - `game reflex <petId>` - start reflex game
   - `achievements` - list all achievements and unlock status
   - Show achievement notification when unlocked

### Technical Notes
- Achievement saga has no ending (tracks player's full lifecycle)
- Mini-game state temporary (not persisted, just in CLI interaction)
- Achievement projection for query display

---

## Phase 9: Advanced Features & Polish

**Goal:** Add sophisticated features and production-ready improvements.

### Deliverables
1. **Snapshot Configuration**
   ```java
   @Aggregate(snapshotTriggerDefinition = "petSnapshotTrigger")
   class Pet {
     // Snapshot every 50 events
   }
   ```

2. **Upcasting** (Event Versioning)
   - Create `PetCreatedEventV2` with additional field
   - Implement `EventUpcaster` to migrate V1 → V2
   - Test with existing event store

3. **Deadline Management**
   ```java
   @DeadlineHandler
   void handle(PetStarvationDeadline deadline) {
     // If pet not fed within 24 hours, emit critical warning
   }
   ```

4. **Dead Letter Queue**
   - Handle failed events in projections
   - Implement retry logic and DLQ monitoring
   - CLI command to view and replay failed events

5. **Replay & Reset**
   - CLI commands to reset projections and replay events
   - `admin reset-projection <name>`
   - `admin replay-events <fromSequence>`

6. **Performance Optimizations**
   - Add caching layer for frequently accessed projections
   - Tune tracking processor batch size
   - Monitor Axon Server metrics

7. **Testing Improvements**
   - `FixtureConfiguration` tests for aggregate
   - Integration tests with test containers
   - Reactive test utilities for time tick system

8. **Docker Production Build**
   - Multi-stage Dockerfile for application
   - Docker compose with production-ready Axon Server config
   - Volume mounts for persistence
   - Health checks and restart policies

### Technical Considerations
- Snapshots reduce replay time for long-lived aggregates
- Upcasting essential for schema evolution
- Deadlines useful for time-based business rules
- Production config different from dev (connection pools, etc.)

---

## Data Model Summary

### Current Aggregates (Phase 1-4)
- **Pet:** petId, name, type, hunger, happiness, health, age, stage, evolutionPath, isAlive, totalTicks

### Current Commands (Phase 1-6)
- CreatePetCommand, FeedPetCommand, PlayWithPetCommand, CleanPetCommand
- TimeTickCommand, EvolvePetCommand

### Current Events (Phase 1-6)
- PetCreatedEvent, PetFedEvent, PetPlayedWithEvent, PetCleanedEvent
- TimePassedEvent, PetHealthDeterioratedEvent, PetDiedEvent
- PetEvolvedEvent

### Current Queries (Phase 1-6)
- GetPetStatusQuery, GetAllPetsQuery, GetAlivePetsQuery, GetPetHistoryQuery
- GetStatisticsQuery, GetLeaderboardQuery

### Current Projections (Phase 1-6)
- PetStatusProjection (current state with JPA persistence)
- PetHistoryProjection (event list from EventStore)
- PetStatisticsProjection (global statistics)

### Future Additions (Phase 5+)
- **Inventory:** inventoryId, items: Map<ItemType, Integer>
- **PlayerAchievements:** playerId, unlockedAchievements: Set<Achievement>
- Additional commands, events, and projections per phase

---

## Configuration Files

### application.yml
```yaml
spring:
  application:
    name: virtual-pet
  datasource:
    url: jdbc:postgresql://localhost:5432/petdb
    username: petuser
    password: petpass
axon:
  axonserver:
    servers: localhost:8124
  eventhandling:
    processors:
      pet-status:
        mode: tracking
        source: eventBus
```

### docker-compose.yml
```yaml
services:
  axon-server:
    image: axoniq/axonserver:latest
    ports:
      - "8024:8024"  # GUI
      - "8124:8124"  # gRPC
    volumes:
      - axon-data:/data
      - axon-events:/eventdata
    networks:
      - pet-network

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: petdb
      POSTGRES_USER: petuser
      POSTGRES_PASSWORD: petpass
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - pet-network

volumes:
  axon-data:
  axon-events:
  postgres-data:

networks:
  pet-network:
```

---

## Testing Strategy

### Unit Tests
- Aggregate behavior with `AggregateTestFixture`
- Test command handlers and event sourcing handlers
- Test business rule validation

### Integration Tests
- Full flow: command → event → projection
- Use Testcontainers for Axon Server and Postgres
- Test saga coordination

### Reactive Tests
- `StepVerifier` for Flux/Mono testing
- Time-based testing with `VirtualTimeScheduler`
- Backpressure scenarios

---

## Future Enhancements (Post-Phase 9)

- **Next.js Frontend:** React-based UI with real-time updates via WebSocket
- **Multi-player:** Multiple users, each with own pets and inventory
- **Pet Interactions:** Pets can interact with each other (visit, gift items)
- **Breeding System:** Combine two adult pets to create egg with traits
- **Marketplace:** Trade items or pets between players
- **Seasonal Events:** Special items/evolutions during holidays
- **Battle System:** Turn-based combat mini-game between pets
- **Cloud Deployment:** Kubernetes deployment with AxonServer SE cluster

---

## Key Learning Outcomes

By completing this project, you will gain hands-on experience with:

✅ **Axon Framework:** Aggregates, commands, events, event sourcing, CQRS, sagas, projections, snapshots, upcasting  
✅ **Project Reactor:** Flux, Mono, reactive streams, backpressure, time-based operations, subscription management  
✅ **Event-Driven Architecture:** Event modeling, bounded contexts, eventual consistency, saga patterns  
✅ **Distributed Systems:** Event stores, projection consistency, replay mechanisms, failure handling  
✅ **Spring Boot Integration:** Configuration, dependency injection, REST APIs, WebSocket, testing  
✅ **Domain-Driven Design:** Aggregates, domain events, ubiquitous language, anti-corruption layers

---

## Development Tips

1. **Start Docker first:** Always run `docker-compose up -d` before starting the app
2. **Check Axon Server UI:** http://localhost:8024 to view events, queries, commands
3. **Event naming:** Use past tense (PetFed, not FeedPet) for events
4. **Keep aggregates pure:** No Spring beans or external dependencies in aggregate
5. **Saga state:** Keep minimal - store only IDs and decision data
6. **Test incrementally:** Each phase should be fully tested before moving to next
7. **Use Axon logging:** Enable debug logging to understand event flow
8. **Projection replay:** Regularly test projection reset and replay during development

---

## Glossary

- **Aggregate:** Domain object that enforces invariants and is the source of events
- **Command:** Request to change state (intent)
- **Event:** Fact that something happened (immutable)
- **Event Sourcing:** Storing state as sequence of events rather than current state
- **CQRS:** Command Query Responsibility Segregation - separate read and write models
- **Saga:** Long-running business process coordinator
- **Projection:** Read model built from events
- **Tracking Processor:** Reads events and updates projections
- **Snapshot:** Cached aggregate state to speed up loading
- **Upcasting:** Converting old event versions to new versions