# Virtual Pet (Tamagotchi) - Axon Framework & Project Reactor
## Design Document

**Game Genre:** Idle/Incremental with Roguelite Mechanics
**Progression Model:** Cookie-clicker style - early game intensive babysitting → mid game automation + strategic trade-offs → late game multi-pet power-leveling

### Technology Stack
- **Language:** Java 21+
- **Framework:** Spring Boot 3.x
- **Event Sourcing/CQRS:** Axon Framework 4.x
- **Reactive:** Project Reactor (reactor-core)
- **Event Store:** Axon Server (via Docker)
- **Build Tool:** Maven
- **Interface:** CLI + REST API (Swagger docs available at `/swagger-ui.html`)

### Major Design Revision (2025-10-28)
**What Changed:**
- Phase 7-9 completely redesigned around XP progression and equipment systems
- Removed: Original consumable-only inventory design, standalone chat/LLM integration, RPG skill trees, dungeon combat
- Added: Global XP system, equippable items with stat modifiers, permanent upgrades, prestige mechanics, power-leveling via XP multipliers
- Focus: Idle/incremental game loop where XP unlocks automation and strategic depth

**Core Loop:**
1. Care for pets → Earn XP (with multipliers)
2. Spend XP → Buy equipment/upgrades/consumables
3. Equipment → Modify pet stats (trade-offs: slower hunger = faster health decay)
4. Automation → Auto-feeder/Auto-play reduce manual actions
5. Power-leveling → Older pets earn more XP, fund new pets faster
6. Prestige → Reset for permanent bonuses, replay with advantages

---

## Architecture Overview

### Core Concepts
- **Aggregate:** `Pet` - the write model, enforces business rules
- **Commands:** User intentions (CreatePet, FeedPet, PlayWithPet, etc.)
- **Events:** Facts that happened (PetCreated, PetFed, PetBecameHungry, etc.)
- **Projections:** Read models for queries (current pet status, statistics)
- **Sagas:** Multi-step workflows (health management, evolution triggers)
- **Reactive Time:** Project Reactor handles scheduled degradation

### Bounded Contexts
1. **Pet Lifecycle** - core pet state and basic needs
2. **Time Management** - handles periodic ticks and degradation
3. **Player Progression** - XP earning, spending, and progression tracking
4. **Inventory & Equipment** - consumable items and equippable gear

---

## Current Project Status

**Active Phase:** Phase 6 Complete - Ready for Phase 7A

**Build Status:** ✅ All checks passing (Checkstyle, SpotBugs, tests)
**Test Suite:** 117 tests passing
**REST API:** ✅ Fully operational with 9 endpoints + Swagger documentation

**Game Vision:** Idle/incremental game with roguelite mechanics. Early game requires active babysitting; mid-game unlocks automation and strategic trade-offs; late game features power-leveling and complex builds.

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

**Next Steps:** Phase 7A - XP System Foundation

### Progression Curve (Designed Play Pattern)

```
Time          | Player Actions                    | XP Rate    | Automation Level
--------------|-----------------------------------|------------|------------------
0-10 min      | Constant babysitting, learning    | 1x base    | None (100% manual)
10-30 min     | First upgrades, breathing room    | 1.5x       | ~10% (equipment passive bonuses)
30-60 min     | Multi-pet management begins       | 2-3x       | ~30% (first automation item)
1-2 hours     | Power-leveling optimization       | 3-5x       | ~50% (multiple auto-items)
2+ hours      | Strategic builds, prestige prep   | 5x+ (cap)  | ~70% (full automation suite)
Post-prestige | Replay with bonuses              | 1.1x base  | Ramps faster due to kept upgrades
```

**Design Goals:**
- First 10 minutes: Player learns mechanics, earns enough XP for meaningful first purchase (~200 XP)
- By 30 minutes: Player has purchased 1-2 automation items, can step away for short periods
- By 1 hour: Player managing 2-3 pets, making strategic equipment decisions
- By 2 hours: Considering prestige, optimizing XP/min, experimenting with builds

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

## Phase 7: Progression & Equipment System (Idle/Incremental Mechanics)

**Goal:** Transform the game into an idle/incremental experience where XP earning unlocks equipment and automation, reducing manual babysitting over time.

**Design Philosophy:** Cookie-clicker progression - start with intensive micromanagement, unlock automation and strategic trade-offs, eventually manage multiple power-leveled pets with complex builds.

### Phase 7A: XP System Foundation

**Goal:** Implement XP earning and tracking without spending mechanics.

#### Deliverables

1. **PlayerProgression Aggregate**
   ```java
   @Aggregate
   class PlayerProgression {
     @AggregateIdentifier
     private String playerId; // "PLAYER" for single-player
     private long totalXP;
     private long lifetimeXPEarned; // Never decreases, for achievements
     private int totalPetsCreated;
     private int prestigeLevel; // Future: prestige mechanics

     // Commands: EarnXPCommand, SpendXPCommand
     // Events: XPEarnedEvent, XPSpentEvent, InsufficientXPEvent
   }
   ```

2. **XP Earning Sources**
   - **Interaction XP:** Feed (+10 XP), Play (+15 XP), Clean (+10 XP)
   - **Survival XP:** 1 XP per tick while pet is alive
   - **Evolution XP:** Egg→Baby (50 XP), Baby→Teen (100 XP), Teen→Adult (200 XP)
   - **Milestone XP:** First pet (100 XP), pet reaches age 100 (150 XP), etc.

3. **Pet XP Multiplier** (Power-leveling mechanic)
   - Pet Aggregate gets new field: `xpMultiplier: double` (starts at 1.0)
   - Multiplier increases by:
     - **Age milestones:** +0.1x every 50 ticks
     - **Care quality:** +0.05x if all stats >70 for 10+ consecutive ticks
     - **Equipment bonuses:** XP Charm item adds +0.25x (future phase)
   - All XP earned from/by this pet is multiplied by its multiplier

4. **XP Earning Saga**
   - Listens to: PetFedEvent, PetPlayedWithEvent, PetCleanedEvent, TimePassedEvent, PetEvolvedEvent
   - Calculates XP based on event type and pet's multiplier
   - Dispatches `EarnXPCommand(playerId, xpAmount, source)`
   - Emits `XPEarnedEvent(playerId, xpAmount, newTotal, source)`

5. **PlayerProgressionProjection**
   - JPA entity tracking current XP balance
   - Handles XPEarnedEvent and XPSpentEvent
   - Query: `GetPlayerProgressionQuery` returns current XP, lifetime XP, pet count

6. **CLI/API Updates**
   - Dashboard shows current XP and lifetime XP
   - Status command shows pet's XP multiplier
   - New endpoint: `GET /api/player/progression`

#### Technical Notes
- XP is global (account-wide), not per-pet
- XP multiplier stored in Pet aggregate (event sourced)
- Saga coordinates XP earning across aggregates
- No spending mechanics yet (Phase 7C)

#### Testing Checklist
- Create pet, verify interaction XP earned
- Let pet survive ticks, verify survival XP accumulates
- Evolve pet, verify evolution XP bonus
- Check XP multiplier increases with age
- Verify XP persists across application restarts

---

### Phase 7B: Equipment System

**Goal:** Introduce equippable items that modify pet stats, creating strategic trade-offs.

#### Deliverables

1. **PlayerInventory Aggregate**
   ```java
   @Aggregate
   class PlayerInventory {
     @AggregateIdentifier
     private String playerId;
     private Map<String, EquippableItem> unequippedItems; // itemId → item
     private Map<ConsumableType, Integer> consumables; // APPLE → 10
     private Set<PermanentUpgrade> upgrades; // Owned permanent upgrades

     // Commands: AddItemCommand, RemoveItemCommand, AddUpgradeCommand
     // Events: ItemAddedEvent, ItemRemovedEvent, UpgradeAddedEvent
   }
   ```

2. **Pet Aggregate Updates**
   ```java
   // Add to Pet aggregate:
   private Map<EquipmentSlot, String> equippedItems; // FOOD_BOWL → "slow-feeder-001"
   private int maxEquipmentSlots; // Increases with evolution: EGG=0, BABY=1, TEEN=2, ADULT=3

   // New commands:
   // EquipItemCommand(petId, itemId, slot)
   // UnequipItemCommand(petId, slot)

   // New events:
   // ItemEquippedEvent(petId, itemId, slot, statModifiers)
   // ItemUnequippedEvent(petId, itemId, slot)
   ```

3. **Equipment Slots**
   - **FOOD_BOWL:** Modifies hunger/feeding mechanics
   - **TOY:** Modifies happiness/play mechanics
   - **ACCESSORY:** Modifies health/general stats
   - Slots unlock: Baby=1 slot, Teen=2 slots, Adult=3 slots

4. **Starter Equipment Items** (Given free at player creation)
   - "Basic Bowl" (FOOD_BOWL): No modifiers, just a placeholder
   - "Simple Toy" (TOY): No modifiers
   - "Comfort Blanket" (ACCESSORY): +1 health per tick

5. **Equipment Stat Modifiers**
   - Items define modifiers: `Map<StatModifierType, Double>`
   - Modifiers: HUNGER_DECAY_RATE, HAPPINESS_DECAY_RATE, HEALTH_DECAY_RATE, FOOD_EFFICIENCY, PLAY_EFFICIENCY, HEALTH_REGEN
   - Applied during TimeTickCommand processing
   - Example: "Slow Feeder" → HUNGER_DECAY_RATE: -0.4 (40% slower), HEALTH_DECAY_RATE: +0.15 (15% faster)

6. **Equip/Unequip Saga**
   - User: `POST /api/pets/{petId}/equip` with `{itemId, slot}`
   - Saga queries PlayerInventory: does player own this item (unequipped)?
   - Saga queries Pet: is slot available (not exceeding maxSlots)?
   - If current item in slot: dispatch UnequipItemCommand → item returns to PlayerInventory
   - Dispatch EquipItemCommand → item removed from PlayerInventory
   - Pet aggregate applies stat modifiers

7. **Death Handling Enhancement**
   - On PetDiedEvent: Saga listens
   - For each equipped item: dispatch UnequipItemCommand
   - Items return to PlayerInventory (not lost)
   - Consumables in pet's inventory ARE lost
   - Emits `PetDeathMourningEvent` → all alive pets owned by player lose 10% happiness

8. **CLI/API Updates**
   - `equipment <petId>` - show equipped items and stat modifiers
   - `equip <petId> <itemId> <slot>` - equip item to pet
   - `unequip <petId> <slot>` - unequip item from pet
   - New endpoints:
     - `GET /api/inventory` - view unequipped items and consumables
     - `POST /api/pets/{id}/equip` - equip item
     - `POST /api/pets/{id}/unequip` - unequip item

#### Testing Checklist
- Equip item to pet, verify stat modifiers applied
- Unequip item, verify modifiers removed
- Pet evolves, verify slot count increases
- Pet dies, verify items return to inventory
- Verify equipped items survive death, consumables don't

---

### Phase 7C: XP Shop

**Goal:** Allow spending XP to purchase equipment and permanent upgrades.

#### Deliverables

1. **Item Catalog** (configuration/database)
   ```java
   @Entity
   class ItemDefinition {
     private String itemId;
     private String name;
     private ItemCategory category; // EQUIPMENT, CONSUMABLE, UPGRADE
     private EquipmentSlot slot; // For equipment
     private Map<StatModifierType, Double> modifiers;
     private int xpCost;
     private String description;
   }
   ```

2. **Equipment Items for Sale**

   **Food Bowl Slot:**
   - "Slow Feeder" (200 XP): hunger decay -40%, health decay +15%
   - "Nutrient Bowl" (300 XP): food items 50% more effective (FOOD_EFFICIENCY +0.5)
   - "Auto-Feeder" (500 XP): auto-feeds when hunger >70 (requires consumables stocked)

   **Toy Slot:**
   - "Toy Box" (200 XP): happiness decay -30%, hunger decay +20%
   - "Exercise Wheel" (300 XP): play actions +50% happiness, +10% hunger
   - "Entertainment System" (500 XP): auto-play every 5 ticks if happiness <50

   **Accessory Slot:**
   - "Cozy Bed" (200 XP): health regen +2/tick, happiness decay +10%
   - "Health Monitor" (400 XP): prevents death once (breaks on save), then returns to inventory
   - "XP Charm" (600 XP): pet's XP multiplier +0.25x

3. **Permanent Upgrades** (Account-wide, apply to all pets)
   - "Efficient Metabolism" (200 XP): all pets hunger decay -10%
   - "Happy Disposition" (150 XP): all pets start with +20 happiness
   - "Sturdy Genetics" (150 XP): all pets start with +20 health
   - "Industrial Kitchen" (500 XP): all food items 2x effective
   - "Multi-Pet License I" (300 XP): can own 2 pets simultaneously
   - "Multi-Pet License II" (600 XP): can own 3 pets (requires License I)
   - "Multi-Pet License III" (1000 XP): can own 4 pets (requires License II)
   - "Fast Hatcher" (250 XP): eggs hatch in 3 ticks instead of 5

4. **Purchase Flow**
   - User: `POST /api/shop/purchase/{itemType}` or `/api/shop/upgrades/{upgradeType}`
   - Command: `PurchaseItemCommand(playerId, itemType, xpCost)`
   - Saga:
     1. Query PlayerProgression: sufficient XP?
     2. If yes: dispatch `SpendXPCommand(playerId, xpCost)`
     3. Wait for `XPSpentEvent`
     4. Dispatch `AddItemCommand` or `AddUpgradeCommand` to PlayerInventory
   - If insufficient: throw `InsufficientXPException` (HTTP 400)

5. **Create Pet XP Cost**
   - First pet: FREE (0 XP)
   - Subsequent pets: 50 XP, 100 XP, 150 XP, ... (increases by 50 each time)
   - Formula: `max(0, (totalPetsCreated - 1) * 50)`
   - Modified by Multi-Pet License upgrades (reduce cost by 25% per tier)

6. **Starter Package** (Given at player creation)
   - 100 starting XP
   - 3 Apples (consumable)
   - 1 Ball (consumable)
   - "Comfort Blanket" (basic accessory)

7. **CLI/API Updates**
   - `shop` - list all available items and upgrades with XP costs
   - `buy <itemType>` - purchase item or upgrade
   - `upgrades` - show owned permanent upgrades
   - New endpoints:
     - `GET /api/shop/items` - browse equipment
     - `GET /api/shop/upgrades` - browse upgrades
     - `POST /api/shop/purchase/item/{itemId}`
     - `POST /api/shop/purchase/upgrade/{upgradeId}`

#### Testing Checklist
- Purchase item, verify XP deducted
- Purchase with insufficient XP, verify rejection
- Purchase permanent upgrade, verify applies to new pets
- Create multiple pets, verify XP cost increases
- Purchase Multi-Pet License, verify can create more pets

---

### Phase 7D: Consumables & Auto-Actions

**Goal:** Introduce consumable items (food, medicine) and automation items that use them.

#### Deliverables

1. **Consumable Items**
   - **Food:**
     - Apple (50 XP): hunger -20
     - Pizza (100 XP): hunger -30, happiness +5
     - Gourmet Meal (200 XP): hunger -40, happiness +10
   - **Medicine:**
     - Basic Medicine (100 XP): health +20
     - Advanced Medicine (200 XP): health +40, cure sickness
   - **Treats:**
     - Cookie (75 XP): happiness +10
     - Premium Toy (150 XP): happiness +20

2. **Using Consumables**
   - Command: `UseConsumableCommand(petId, playerId, consumableType)`
   - Saga:
     1. Query PlayerInventory: has consumable?
     2. Dispatch `RemoveItemCommand` to PlayerInventory
     3. Dispatch appropriate Pet command (FeedPetCommand, HealPetCommand, PlayWithPetCommand)
     4. Apply item effects (modified by equipment like "Nutrient Bowl")

3. **Auto-Action Equipment**
   - Items like "Auto-Feeder" and "Entertainment System" trigger during TimeTickCommand
   - Check conditions (hunger >70, happiness <50)
   - Query PlayerInventory for consumables
   - If available: auto-dispatch UseConsumableCommand
   - If unavailable: item does nothing (no error)

4. **Sickness Mechanic** (Enhanced depth)
   - If health <30 for 3+ consecutive ticks: 20% chance per tick of `PetBecameSickEvent`
   - Sickness effects: hunger decay +50%, happiness decay +50%, cannot play
   - Advanced Medicine cures sickness immediately
   - Basic Medicine does not cure sickness (only restores health)

5. **CLI/API Updates**
   - `use <petId> <consumableType>` - manually use consumable
   - `craft <consumableType> <quantity>` - buy consumables with XP
   - Status shows if pet is sick
   - New endpoint: `POST /api/pets/{id}/use` - use consumable on pet

#### Testing Checklist
- Use consumable, verify inventory decreases
- Auto-Feeder triggers when hunger high
- Auto-Feeder doesn't trigger when no consumables
- Pet becomes sick at low health
- Advanced medicine cures sickness
- Equipment modifiers affect consumable effectiveness

---

### Phase 7E: Power-Leveling & Balance Tuning

**Goal:** Fine-tune XP curves, equipment balance, and test multi-pet power-leveling strategies.

#### Deliverables

1. **XP Multiplier Enhancements**
   - Cap multiplier growth at 5.0x (prevent infinite scaling)
   - Multiplier decays slowly if pet stats drop below 50 for extended time (-0.05x per 10 ticks)
   - "Perfect Care" achievement: maintain 3.0x+ multiplier for 100 ticks

2. **Equipment Synergies** (future enhancement hooks)
   - Track which items are frequently equipped together
   - Placeholder for future "set bonuses"

3. **Balance Testing Scenarios**
   - Early game: Can you survive first pet to adult with starter package?
   - Mid game: Can you afford first automation item (Auto-Feeder) by age 100?
   - Late game: Can you maintain 3+ pets simultaneously with power-leveling?

4. **Dashboard Enhancements**
   - Show XP earned per minute (last 10 ticks average)
   - Show highest XP multiplier across all pets
   - Show total XP spent vs earned

5. **CLI/API Polish**
   - Color-code equipment by rarity/cost tier
   - Show item recommendations based on current pet stats
   - Add `/api/analytics/xp-rate` endpoint for progression tracking

#### Testing Checklist
- Verify XP multiplier caps at 5.0x
- Test death penalty (happiness loss to other pets)
- Balance check: early game progression feels achievable
- Balance check: automation unlocks feel rewarding
- Verify multi-pet XP farming strategies work

---

## Phase 8: Achievements & Mini-Games

**Goal:** Track player progression through achievements and add interactive mini-games that grant XP bonuses.

### Deliverables

1. **Achievement Aggregate** (merged with PlayerProgression from Phase 7A)
   ```java
   // Extend PlayerProgression aggregate:
   private Set<Achievement> unlockedAchievements;

   // Commands: UnlockAchievementCommand
   // Events: AchievementUnlockedEvent(achievementId, xpBonus)
   ```

2. **Achievement Types** (XP progression-focused)

   **Care Achievements:**
   - **First Pet** (100 XP bonus): Create your first pet
   - **Veteran Owner** (500 XP bonus): Create 10 pets (dead or alive)
   - **Perfect Care** (300 XP bonus): Evolve pet to adult with all stats >90
   - **Survivor** (400 XP bonus): Keep a pet alive for 500 ticks

   **Progression Achievements:**
   - **XP Millionaire** (1000 XP bonus): Earn 10,000 lifetime XP
   - **Power Leveler** (500 XP bonus): Reach 3.0x XP multiplier on a pet
   - **Multi-Pet Master** (300 XP bonus): Own 3+ pets simultaneously

   **Equipment Achievements:**
   - **Collector** (200 XP bonus): Own 10+ different equipment items
   - **Big Spender** (500 XP bonus): Spend 5,000 total XP in the shop
   - **Automation Expert** (300 XP bonus): Equip Auto-Feeder and Entertainment System on same pet

   **Mastery Achievements:**
   - **Healer** (200 XP bonus): Use medicine 20 times
   - **Gourmet** (200 XP bonus): Feed pets premium items 50 times
   - **Death Defier** (500 XP bonus): Trigger Health Monitor's death prevention

3. **Achievement Tracking Saga**
   - Listens to all pet, progression, and inventory events
   - Maintains counters in saga state (items used, XP earned, pets created, etc.)
   - When criteria met: dispatch `UnlockAchievementCommand`
   - On unlock: grant XP bonus via `EarnXPCommand`
   - Display toast notification in UI

4. **Mini-Game Commands** (XP Boosters)
   - `PlayGuessGameCommand(petId, playerGuess)`
     - Guess number 1-5
     - Success: +20 happiness, +30 XP (affected by pet multiplier)
     - Failure: +5 happiness, +10 XP
     - Cooldown: 5 ticks between plays

   - `PlayReflexGameCommand(petId, startTime)`
     - Player must respond within time window
     - Fast (<1s): +25 happiness, +50 XP
     - Medium (1-2s): +15 happiness, +30 XP
     - Slow (>2s): +10 happiness, +15 XP
     - Cooldown: 5 ticks between plays

   - `PlayEmotionalIntelligenceGameCommand(petId, action)`
     - Pet shows mood, player interprets correct need
     - Correct: +30 happiness, +40 XP
     - Wrong: +5 happiness, +10 XP
     - Cooldown: 10 ticks between plays

5. **Mini-Game State**
   - Store in Pet aggregate: `Map<GameType, Instant> lastPlayedTime`
   - Business rule: cannot play if cooldown not elapsed
   - Mini-game outcomes trigger XP earning saga

6. **CLI/API Updates**
   - `achievements` - list all achievements and unlock status
   - `game guess <petId>` - play guess game
   - `game reflex <petId>` - play reflex game
   - `game emotional <petId>` - play emotional intelligence game
   - New endpoints:
     - `GET /api/achievements` - list achievements
     - `POST /api/games/{petId}/guess` - play guess game
     - `POST /api/games/{petId}/reflex` - play reflex game
     - `POST /api/games/{petId}/emotional` - play emotional game

### Technical Notes
- Achievement unlocks grant one-time XP bonuses
- Mini-games are alternative XP sources (higher reward but cooldown-gated)
- Achievements tracked via saga with no ending (player-lifecycle)
- Achievement projection for query/display

### Testing Checklist
- Unlock achievement, verify XP bonus granted
- Play mini-game successfully, verify XP earned with multiplier
- Attempt mini-game during cooldown, verify rejection
- Check achievement progress tracking across multiple pets
- Verify achievement notifications display correctly

---

## Phase 9: Prestige System & Advanced Polish

**Goal:** Add prestige mechanics for long-term replayability and production-ready improvements.

### Deliverables

1. **Prestige System**
   ```java
   // Extend PlayerProgression aggregate:
   private int prestigeLevel;
   private Map<String, Double> prestigeBonuses; // Permanent multipliers

   // Commands: PrestigeCommand
   // Events: PlayerPrestigedEvent(level, bonusesGranted)
   ```

   **Prestige Mechanics:**
   - Can prestige when lifetime XP earned >50,000
   - Prestige resets: all pets (delete), all equipment (remove), current XP (to 0)
   - Prestige keeps: permanent upgrades, achievements, lifetime XP counter
   - Prestige grants: +10% XP earning multiplier per level (stacks)
   - Prestige grants: +5% stat decay reduction per level (stacks)
   - Prestige Level displayed as badge/title

2. **Snapshot Configuration** (performance optimization)
   ```java
   @Aggregate(snapshotTriggerDefinition = "petSnapshotTrigger")
   class Pet {
     // Snapshot every 100 events to speed up replay
   }

   @Aggregate(snapshotTriggerDefinition = "progressionSnapshotTrigger")
   class PlayerProgression {
     // Snapshot every 50 events (XP transactions)
   }
   ```

3. **Performance Optimizations**
   - Add Redis caching layer for PlayerProgressionProjection (high read frequency)
   - Tune tracking processor batch sizes for projections
   - Index database columns: petId, playerId, isAlive, stage
   - Monitor Axon Server metrics via dashboard

4. **Testing Improvements**
   - `AggregateTestFixture` for all aggregates
   - Integration tests with Testcontainers (Axon Server + Postgres)
   - Reactive testing with `StepVerifier` for time tick system
   - Load testing: 100+ pets, 10,000+ events

5. **Docker Production Build**
   - Multi-stage Dockerfile for application
   - Docker compose with production-ready Axon Server config
   - Volume mounts for data persistence
   - Health checks and restart policies
   - Environment-based configuration (dev/staging/prod)

6. **Admin Tools**
   - CLI commands:
     - `admin reset-projection <name>` - reset and replay projection
     - `admin grant-xp <amount>` - cheat command for testing
     - `admin prestige-player` - force prestige for testing
   - Endpoints:
     - `POST /api/admin/reset-projection/{name}`
     - `POST /api/admin/grant-xp`

### Technical Considerations
- Prestige adds long-term replayability (meta-progression)
- Snapshots reduce replay time for aggregates with many events
- Caching critical for high-traffic projections
- Production requires proper health checks and monitoring

---

## Data Model Summary

### Aggregates (Phase 1-9)

**Pet Aggregate** (Phase 1-7)
- Core fields: petId, name, type, hunger, happiness, health, age, stage, evolutionPath, isAlive, totalTicks
- Phase 7 additions: xpMultiplier, equippedItems, maxEquipmentSlots
- Phase 8 additions: lastPlayedGames (cooldown tracking)

**PlayerProgression Aggregate** (Phase 7A-9)
- Core fields: playerId, totalXP, lifetimeXPEarned, totalPetsCreated
- Phase 8 addition: unlockedAchievements
- Phase 9 addition: prestigeLevel, prestigeBonuses

**PlayerInventory Aggregate** (Phase 7B-C)
- Fields: playerId, unequippedItems, consumables, permanentUpgrades

### Commands (Phase 1-9)

**Pet Commands:**
- CreatePetCommand, FeedPetCommand, PlayWithPetCommand, CleanPetCommand
- TimeTickCommand, EvolvePetCommand
- EquipItemCommand, UnequipItemCommand
- PlayGuessGameCommand, PlayReflexGameCommand, PlayEmotionalIntelligenceGameCommand
- UseConsumableCommand

**Progression Commands:**
- EarnXPCommand, SpendXPCommand
- UnlockAchievementCommand, PrestigeCommand

**Inventory Commands:**
- AddItemCommand, RemoveItemCommand, AddUpgradeCommand
- PurchaseItemCommand, PurchaseUpgradeCommand

### Events (Phase 1-9)

**Pet Events:**
- PetCreatedEvent, PetFedEvent, PetPlayedWithEvent, PetCleanedEvent
- TimePassedEvent, PetHealthDeterioratedEvent, PetDiedEvent, PetEvolvedEvent
- ItemEquippedEvent, ItemUnequippedEvent
- PetBecameSickEvent, PetCuredEvent
- PetDeathMourningEvent, GamePlayedEvent

**Progression Events:**
- XPEarnedEvent, XPSpentEvent, InsufficientXPEvent
- AchievementUnlockedEvent, PlayerPrestigedEvent

**Inventory Events:**
- ItemAddedEvent, ItemRemovedEvent, UpgradeAddedEvent
- ItemPurchasedEvent, UpgradePurchasedEvent

### Queries (Phase 1-9)
- GetPetStatusQuery, GetAllPetsQuery, GetAlivePetsQuery, GetPetHistoryQuery
- GetStatisticsQuery, GetLeaderboardQuery
- GetPlayerProgressionQuery, GetPlayerInventoryQuery
- GetAchievementsQuery, GetShopCatalogQuery

### Projections (Phase 1-9)
- PetStatusProjection (current pet state with JPA)
- PetHistoryProjection (event history from EventStore)
- PetStatisticsProjection (global statistics)
- PlayerProgressionProjection (XP, achievements, prestige)
- PlayerInventoryProjection (items, upgrades, consumables)
- ShopCatalogProjection (available items and prices)

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

### Immediate Priorities
- **WebSocket Updates:** Real-time frontend updates instead of polling
- **Enhanced Prestige:** More prestige tiers, special rewards, cosmetic unlocks
- **Equipment Sets:** Bonus effects when equipping related items together
- **Seasonal Events:** Limited-time items, special evolutions, event achievements

### Long-Term Vision
- **Multi-player:** Multiple users, each with own progression and pets
- **Pet Interactions:** Pets visit each other, share happiness bonuses
- **Breeding System:** Combine two adult pets to create egg with inherited traits
- **Marketplace:** Trade equipment between players (not consumables)
- **Battle System:** Turn-based combat using pet stats + equipment bonuses
- **Guild/Social:** Group progression, shared achievements, leaderboards
- **Cloud Deployment:** Kubernetes deployment with AxonServer SE cluster
- **Mobile App:** Native iOS/Android with push notifications for critical stats

### Explicitly Not Planned (conflicts with current design)
- ❌ LLM chat integration (adds complexity without fitting idle game loop)
- ❌ RPG skill trees (replaced by equipment system)
- ❌ Dungeon crawling (replaced by mini-games + achievements)

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