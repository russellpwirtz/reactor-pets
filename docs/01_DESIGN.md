# Virtual Pet (Tamagotchi) - Axon Framework & Project Reactor
## Design Document

### Technology Stack
- **Language:** Java 21+
- **Framework:** Spring Boot 3.x
- **Event Sourcing/CQRS:** Axon Framework 4.x
- **Reactive:** Project Reactor (reactor-core)
- **Event Store:** Axon Server (via Docker)
- **Build Tool:** Maven
- **Interface:** CLI (Phase 1-4), REST API ready for future Next.js frontend

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

**Active Phase:** Phase 1 Complete - Ready for Phase 2

**Build Status:** ✅ All checks passing (Checkstyle, SpotBugs, tests)
**Test Coverage:** 97% (aggregate), 100% (projection), 47% overall (infrastructure excluded)
**Test Suite:** 42 tests passing

**Available Commands:**
- `create <name> <type>` - Create new pet (DOG, CAT, or DRAGON)
- `feed <petId>` - Feed pet to reduce hunger
- `status <petId>` - View current pet status
- `help` - Display command reference
- `exit` - Terminate application

**Next Steps:** Proceed with Phase 2 (Multiple Interactions & State Projections)

---

## Phase 1: Foundation & Basic Pet Lifecycle ✅ COMPLETED

**Goal:** Runnable project with Axon Server, basic Pet aggregate, and ability to create/feed a pet via CLI.

**Status:** Fully implemented and tested with 42 passing tests covering aggregate behavior, projections, and integration scenarios.

### Completed Features
- Pet Aggregate with core attributes (hunger, happiness, health, stage, type)
- Event Sourcing implementation with PetCreatedEvent and PetFedEvent
- CQRS with commands (CreatePetCommand, FeedPetCommand) and queries (GetPetStatusQuery)
- In-memory projection for pet status (PetStatusProjection)
- Interactive CLI for pet interactions (create, feed, status)
- Docker setup with Axon Server (ports 8024 GUI, 8124 gRPC)
- Developer tooling (Spotless, Checkstyle, SpotBugs, JaCoCo with appropriate exclusions)
- Comprehensive test suite (aggregate tests, projection tests, integration tests, smoke tests)

---

## Phase 2: Multiple Interactions & State Projections

**Goal:** Add more interactions (play, clean) and persist projections to database.

### Deliverables
1. **New Commands & Events**
   - `PlayWithPetCommand(petId)` → `PetPlayedWithEvent(petId, happinessIncrease, hungerIncrease, timestamp)`
   - `CleanPetCommand(petId)` → `PetCleanedEvent(petId, healthIncrease, timestamp)`

2. **Enhanced Aggregate Logic**
   - Playing increases happiness (+15) but also increases hunger (+5)
   - Cleaning increases health (+10) if health < 100
   - Add business rule validation (can't play if happiness already at 100, etc.)

3. **Database Persistence for Projections**
   - Add H2/PostgreSQL to docker-compose
   - Spring Data JPA entities for `PetStatusView`
   - Convert projection from in-memory to JPA repository
   - Projection stores: petId, name, type, hunger, happiness, health, stage, isAlive, lastUpdated

4. **Event History Query**
   ```java
   @QueryHandler
   List<PetEvent> handle(GetPetHistoryQuery query) {
     // Return list of all events for a pet (limit to last 50)
   }
   ```

5. **CLI Enhancements**
   - `play <petId>` - play with pet
   - `clean <petId>` - clean pet
   - `history <petId>` - show last 10 events
   - `list` - show all pets

### Technical Notes
- Use `@EventSourcingHandler` vs `@EventHandler` distinction
- Projection subscribes to event stream via tracking event processor
- Event history retrieved via `EventStore` API

---

## Phase 3: Reactive Time System & Degradation

**Goal:** Implement Project Reactor for time-based stat degradation (hunger increases, happiness decreases over time).

### Deliverables
1. **Time Tick System**
   ```java
   @Component
   class TimeTickScheduler {
     @PostConstruct
     void startTimeFlow() {
       Flux.interval(Duration.ofSeconds(10)) // tick every 10 seconds
         .flatMap(tick -> queryForAlivePets())
         .flatMap(petId -> sendTimeTick(petId))
         .subscribe();
     }
   }
   ```

2. **New Command & Event**
   - `TimeTickCommand(petId, tickCount)` → `TimePassedEvent(petId, hungerIncrease, happinessDecrease, ageIncrease, timestamp)`

3. **Aggregate Updates**
   - Time tick increases hunger (+3), decreases happiness (-2)
   - Track pet age in "ticks" (every 10 ticks = 1 age unit)
   - Add `private int age` and `private int totalTicks` to Pet aggregate

4. **Health Deterioration Logic**
   - If hunger > 80, health decreases (-5 per tick)
   - If happiness < 20, health decreases (-3 per tick)
   - Emit `PetHealthDeterioratedEvent` when health drops

5. **Death Mechanic**
   - If health reaches 0, emit `PetDiedEvent`
   - Set `isAlive = false`, stop processing further commands except queries
   - Dead pets still queryable but cannot accept action commands

6. **CLI Updates**
   - Status display shows age and time since last interaction
   - Visual indicators for critical stats (🔴 if hunger > 70, etc.)

### Technical Considerations
- Use `QueryGateway` in reactive pipeline to find alive pets
- Consider backpressure - if system slow, tick might pile up
- TimeTickCommand should be idempotent (include tick sequence number)

---

## Phase 4: Evolution System & Pet Stages

**Goal:** Pets evolve through stages based on age and care quality, implementing Saga pattern.

### Deliverables
1. **Evolution Saga**
   ```java
   @Saga
   class PetEvolutionSaga {
     @StartingSagaEventHandler(associationProperty = "petId")
     void on(PetCreatedEvent event) { /* start tracking */ }
     
     @SagaEventHandler(associationProperty = "petId")
     void on(TimePassedEvent event) {
       // Check if evolution criteria met
       // Dispatch EvolveCommand if yes
     }
     
     @EndingEventHandler
     void on(PetEvolvedEvent event) { /* end saga or continue tracking */ }
   }
   ```

2. **Evolution Criteria**
   - **EGG → BABY:** Age >= 5 (automatic)
   - **BABY → TEEN:** Age >= 20, average happiness > 50
   - **TEEN → ADULT:** Age >= 50, average health > 60, average happiness > 60
   - Track care quality in saga state (rolling average of last 50 ticks)

3. **New Command & Event**
   - `EvolvePetCommand(petId, newStage, evolutionReason)` → `PetEvolvedEvent(petId, oldStage, newStage, evolutionPath, timestamp)`

4. **Evolution Paths**
   - Good care → "Healthy" variant (higher base stats)
   - Poor care → "Neglected" variant (lower base stats)
   - Store evolution path in aggregate: `private EvolutionPath evolutionPath`

5. **Stat Adjustments on Evolution**
   - When evolving, adjust max caps and degradation rates
   - Adults: hunger increases slower (+2/tick vs +3), happiness more stable
   - Neglected path: faster degradation throughout life

6. **CLI Features**
   - `status` shows stage and evolution path
   - ASCII art per stage/type combination (simple text art)
   - Evolution announcement message when it happens

### Technical Notes
- Saga maintains association with petId
- Use `@StartingSagaEventHandler` and `@SagaEventHandler`
- Saga state stores: petId, currentStage, careHistory[]
- Query projection for care quality metrics

---

## Phase 5: Multiple Pets & Statistics Dashboard

**Goal:** Support multiple concurrent pets and aggregate statistics across all pets.

### Deliverables
1. **Pet Manager Service**
   ```java
   @Service
   class PetManagerService {
     // Create multiple pets
     // Query all active pets
     // Global statistics
   }
   ```

2. **New Projections**
   - **Active Pets Projection:** List of all alive pets with key stats
   - **Statistics Projection:** 
     - Total pets created
     - Total pets died
     - Average lifespan
     - Longest-lived pet
     - Evolution stage distribution

3. **Enhanced Time Tick**
   - Batch processing: send tick to all alive pets in single Flux pipeline
   - Error handling: if one pet fails, others continue
   - Use `Flux.fromIterable(alivePets).flatMap(...)` with concurrency control

4. **CLI Dashboard**
   - `dashboard` - shows global stats + list of all pets
   - `leaderboard` - pets sorted by age/happiness
   - Color-coded health indicators per pet

5. **Event Handler Optimizations**
   - Use `@ProcessingGroup` annotations for parallel processing
   - Configure tracking processors in application.yml
   - Add replay capability for statistics projection

### Technical Considerations
- Multiple pet aggregates managed by Axon's repository
- Each pet has unique aggregate ID (UUID)
- Projections listen to all pet events across aggregates
- Consider memory: if 100s of pets, ensure projection efficiency

---

## Phase 6: Items System & Inventory Bounded Context

**Goal:** Introduce food types, toys, and medicine as separate bounded context with inventory management.

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

## Phase 7: Mini-Games & Achievement System

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

## Phase 8: REST API & JSON Interface

**Goal:** Expose REST API for all operations, preparing for Next.js frontend integration.

### Deliverables
1. **REST Controllers**
   ```java
   @RestController
   @RequestMapping("/api/pets")
   class PetController {
     // POST /api/pets - create pet
     // GET /api/pets/{id} - get status
     // POST /api/pets/{id}/feed - feed
     // POST /api/pets/{id}/play - play
     // GET /api/pets - list all
   }
   ```

2. **Endpoints**
   - **Pet Operations:** `/api/pets/*`
   - **Inventory:** `/api/inventory/*`
   - **Achievements:** `/api/achievements`
   - **Statistics:** `/api/stats`
   - **Events:** `/api/pets/{id}/history`

3. **WebSocket Support** (Optional)
   ```java
   @Configuration
   class WebSocketConfig {
     // Subscribe to pet events in real-time
     // Push updates to connected clients
     // Use Project Reactor's Flux for SSE or WebSocket
   }
   ```

4. **DTOs & Serialization**
   - Create request/response DTOs for clean JSON
   - Map commands to DTOs
   - Map projections to view DTOs

5. **Error Handling**
   - Global exception handler for Axon exceptions
   - Return proper HTTP status codes (404, 400, 500)
   - JSON error responses with message

6. **CORS Configuration**
   - Allow localhost:3000 for Next.js dev server
   - Proper headers for REST and WebSocket

7. **API Documentation**
   - Swagger/OpenAPI integration
   - Available at `/swagger-ui.html`

### Technical Notes
- Keep CLI functional alongside REST API
- Use `@Async` for command dispatch if needed
- Subscribe to query results reactively using `subscriptionQueryResult.updates()`

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

### Aggregates
- **Pet:** petId, name, type, hunger, happiness, health, age, stage, evolutionPath, isAlive, totalTicks
- **Inventory:** inventoryId, items: Map<ItemType, Integer>
- **PlayerAchievements:** playerId, unlockedAchievements: Set<Achievement>

### Commands (Examples)
- CreatePetCommand, FeedPetCommand, PlayWithPetCommand, CleanPetCommand
- TimeTickCommand, EvolvePetCommand, GiveMedicineCommand
- AddItemCommand, UseItemCommand
- PlayGuessGameCommand, UnlockAchievementCommand

### Events (Examples)
- PetCreatedEvent, PetFedEvent, PetPlayedWithEvent, TimePassedEvent
- PetEvolvedEvent, PetDiedEvent, PetBecameSickEvent
- ItemAddedEvent, ItemUsedEvent
- AchievementUnlockedEvent

### Projections
- PetStatusProjection (current state)
- PetHistoryProjection (event list)
- StatisticsProjection (aggregates across all pets)
- InventoryProjection (current items)
- AchievementProjection (unlocked achievements)

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