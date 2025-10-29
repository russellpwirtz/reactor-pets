# Reactor Pets - Implementation Roadmap

**Last Updated:** 2025-10-29
**Current Phase:** Phase 7C Complete (Shop System) → Phase 7D In Progress (Consumables)

---

## Quick Reference

### Completed Phases ✅
- ✅ **Phase 1-6:** Foundation, lifecycle, time system, evolution, multiple pets, REST API
- ✅ **Phase 7A:** XP System Foundation
- ✅ **Phase 7B:** Equipment System
- ✅ **Phase 7C:** XP Shop (Domain models, saga, REST endpoints - CLI pending)

### Current Phase 🚧
**Phase 7D: Consumables & Auto-Actions** (Session 1 of 2-3)
- ✅ Consumable domain model (7 types: Apple, Pizza, Gourmet Meal, Basic Medicine, Advanced Medicine, Cookie, Premium Toy)
- ✅ PlayerInventory extended for consumables (quantity tracking)
- ✅ Pet sickness state (isSick, lowHealthTicks)
- ✅ UseConsumableCommand with effects (hunger/happiness/health restore)
- ✅ ConsumableUsageSaga (inventory coordination)
- ✅ Equipment modifiers apply to consumables
- 🚧 Sickness mechanic in TimeTickCommand (health <30 for 3+ ticks → 20% sick chance)
- 🚧 Auto-actions (Auto-Feeder at hunger >70, Entertainment System at happiness <50)
- ⏳ Add consumables to shop
- ⏳ CLI/REST endpoints for consumables
- ⏳ Unit and integration tests

### Upcoming Phases 📋
- **Phase 7E:** Power-Leveling & Balance Tuning (1 session)
- **Phase 8:** Achievements & Mini-Games (2-3 sessions)
- **Phase 9:** Prestige System & Advanced Polish (2-3 sessions)

---

## Phase 7C: XP Shop ✅ COMPLETED

**Implemented:**
- ✅ ItemDefinition, ItemType (EQUIPMENT, PERMANENT_UPGRADE), ShopCatalog
- ✅ 9 Equipment items across 3 slots (Food Bowl, Toy, Accessory)
- ✅ 8 Permanent upgrades (metabolism, disposition, genetics, kitchen, hatcher, multi-pet licenses)
- ✅ ShopPurchaseSaga with XP validation
- ✅ PetCreationService with XP cost logic (FREE, 50, 100, 150...)
- ✅ ShopProjection with query handlers
- ✅ REST endpoints: GET /api/shop/items, /api/shop/upgrades, POST /api/shop/purchase/*
- ✅ Comprehensive unit tests (ShopPurchaseSagaTest)

**Pending:**
- CLI commands for browsing shop and purchasing

---

## Phase 7D: Consumables & Auto-Actions 🚧 IN PROGRESS

**Goal:** Introduce consumable items and automation mechanics

### Completed This Session ✅

**Consumable Domain Model:**
- `ConsumableType` enum with 7 types
- `Consumable` class with effects (hunger/happiness/health restore, cures sickness)
- `ConsumableCatalog` with XP costs:
  - Food: Apple (50 XP), Pizza (100 XP), Gourmet Meal (200 XP)
  - Medicine: Basic Medicine (100 XP), Advanced Medicine (200 XP)
  - Treats: Cookie (75 XP), Premium Toy (150 XP)

**PlayerInventory Extensions:**
- `Map<ConsumableType, Integer> consumables` for quantity tracking
- `AddConsumableCommand`, `RemoveConsumableCommand`
- `ConsumableAddedEvent`, `ConsumableRemovedEvent`
- Command handlers with validation

**Pet Sickness System:**
- Added `isSick` and `lowHealthTicks` fields
- `PetBecameSickEvent`, `PetCuredEvent`
- Event handlers for sickness state

**Consumable Usage:**
- `UseConsumableCommand` (petId, consumableType, playerId)
- `ConsumableUsedEvent` (tracks restoration amounts and cure)
- Command handler applies equipment modifiers (FOOD_EFFICIENCY, PLAY_EFFICIENCY)
- Advanced Medicine cures sickness
- Sick pets cannot use toys

**Saga Coordination:**
- `ConsumableUsageSaga` validates inventory and removes consumable before use

### Next Session TODO 📝

1. **Sickness Mechanic in TimeTickCommand:**
   - Track lowHealthTicks when health <30
   - 20% chance per tick to emit PetBecameSickEvent after 3+ low health ticks
   - Sick pets: hunger/happiness decay +50%, cannot play

2. **Auto-Action Logic in TimeTickCommand:**
   - Query equipped items for Auto-Feeder and Entertainment System
   - Auto-Feeder: hunger >70 → query inventory for food consumables → use Apple/Pizza/Gourmet Meal
   - Entertainment System: happiness <50 → query inventory for treats → use Cookie/Premium Toy
   - Silent when no consumables available

3. **Shop Integration:**
   - Add CONSUMABLE to ItemType enum
   - Extend ShopCatalog with consumables
   - Update ShopPurchaseSaga to handle consumable purchases
   - Add to starter package (3 Apples)

4. **CLI/REST:**
   - CLI: `use <petId> <consumableType>`
   - REST: `POST /api/pets/{id}/consumable/{type}`
   - Update shop endpoints to include consumables

5. **Testing:**
   - Unit tests: Pet aggregate with UseConsumableCommand
   - Unit tests: Sickness mechanic
   - Integration tests: Auto-actions trigger correctly
   - Integration tests: Equipment modifiers affect consumables

---

## Phase 7E: Power-Leveling & Balance Tuning

**Duration:** 1 Claude Code session
**Goal:** Fine-tune progression curves and test multi-pet strategies

**Key Deliverables:**
- XP multiplier cap at 5.0x (prevent infinite scaling)
- Multiplier decay: -0.05x per 10 ticks if stats <50
- Dashboard enhancements: XP/min rate, highest multiplier, XP spent vs earned
- Balance testing scenarios:
  - Early game: survive to adult with starter package
  - Mid game: afford Auto-Feeder by age 100
  - Late game: maintain 3+ pets with power-leveling

---

## Phase 8: Achievements & Mini-Games

**Duration:** 2-3 Claude Code sessions
**Goal:** Track progression through achievements, add mini-games for XP bonuses

**Key Deliverables:**
- Extend PlayerProgression aggregate with achievements set
- 12 achievement types across 4 categories (care, progression, equipment, mastery)
- Each achievement grants one-time XP bonus (100-1000 XP)
- 3 mini-games:
  - Guess Game: 5 tick cooldown, success +30 XP, failure +10 XP
  - Reflex Game: 5 tick cooldown, fast +50 XP, medium +30 XP, slow +15 XP
  - Emotional Intelligence: 10 tick cooldown, correct +40 XP, wrong +10 XP
- Pet aggregate tracks lastPlayedGames for cooldown enforcement
- All mini-game XP affected by pet's multiplier

---

## Phase 9: Prestige System & Advanced Polish

**Duration:** 2-3 Claude Code sessions
**Goal:** Add prestige mechanics and production-ready improvements

**Key Deliverables:**
- Prestige mechanics:
  - Available at 50,000 lifetime XP
  - Resets: all pets deleted, equipment removed, current XP to 0
  - Keeps: permanent upgrades, achievements, lifetime XP counter
  - Grants: +10% XP multiplier per level (stacks), +5% decay reduction per level
- Snapshot configuration for Pet and PlayerProgression aggregates
- Performance optimizations: Redis caching, database indexes, batch tuning
- Testing improvements: AggregateTestFixture, Testcontainers, load testing (100+ pets)
- Production Docker build with health checks

---

## Post-Phase 9 Enhancements

### Immediate Priorities
1. **WebSocket Updates** - Replace polling with real-time events
2. **Enhanced Prestige** - More tiers, cosmetic rewards
3. **Equipment Sets** - Bonuses for related items
4. **Seasonal Events** - Limited items, special evolutions

### Long-Term Vision
1. **Multi-player** - User accounts, separate progressions
2. **Pet Interactions** - Visiting, happiness sharing
3. **Breeding System** - Inherited traits
4. **Marketplace** - Trade equipment (not consumables)
5. **Battle System** - Turn-based combat with stats + equipment
6. **Guild/Social** - Group progression, leaderboards
7. **Cloud Deployment** - Kubernetes + AxonServer SE cluster
8. **Mobile App** - Native iOS/Android with push notifications

---

## Development Workflow

### For Each Phase
1. **Plan:** Review deliverables, identify aggregates/commands/events
2. **Implement:** Write aggregates, commands, events, sagas, projections
3. **Test:** Write unit tests (AggregateTestFixture), integration tests
4. **API:** Add REST endpoints, update Swagger docs
5. **CLI:** Add/update CLI commands for new features
6. **Validate:** Manual testing of full flow via CLI + API
7. **Commit:** Git commit with descriptive message

### Quality Standards
- All new code passes Checkstyle + SpotBugs
- Test coverage >80% for new code (JaCoCo)
- All REST endpoints documented in Swagger
- Event sourcing tested (command → event → projection flow)
- Saga coordination tested for cross-aggregate operations

---

## Success Metrics

### Phase 7 Success
- [x] Player can earn XP from interactions (7A)
- [x] Equipment modifies pet stats with trade-offs (7B)
- [x] Can purchase equipment and upgrades with XP (7C)
- [x] Player progression tracked and persisted (7A)
- [ ] Consumables provide immediate benefits (7D - in progress)
- [ ] Automation items reduce manual actions (7D - pending)
- [ ] Multi-pet power-leveling strategies viable (7E)

### Phase 8 Success
- [ ] Achievements unlock with XP bonuses
- [ ] Mini-games provide alternative XP source
- [ ] Cooldowns prevent mini-game spam

### Phase 9 Success
- [ ] Prestige system functional
- [ ] Prestige bonuses apply on reset
- [ ] Load test: 100+ pets stable

---

## Implementation Notes

**Phase 7C Shop System:**
- ShopCatalog is static (no aggregate), query-only
- PetCreationService coordinates XP spending before pet creation
- Multi-Pet Licenses have prerequisite validation (must buy I before II, etc.)

**Phase 7D Consumables (Current):**
- Consumables are quantity-based (unlike equipment which is instance-based)
- Equipment modifiers apply: FOOD_EFFICIENCY boosts food consumables, PLAY_EFFICIENCY boosts toys
- Sickness prevents play action but allows consumable usage
- Auto-actions will use cheapest available consumable first (Apple before Pizza)

**Axon Patterns:**
- Heavy use of sagas for coordination (Shop, Equipment, Consumables)
- Projections for queries (read models)
- Static catalogs for game data (Shop, Equipment, Consumables)
