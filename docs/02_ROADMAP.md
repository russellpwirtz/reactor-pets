# Reactor Pets - Implementation Roadmap

**Last Updated:** 2025-10-28
**Current Phase:** Phase 7B Complete → Phase 7C Next

---

## Quick Reference

### Completed Phases ✅
- ✅ **Phase 1:** Foundation & Basic Pet Lifecycle
- ✅ **Phase 2:** Multiple Interactions & State Projections
- ✅ **Phase 3:** Reactive Time System & Degradation
- ✅ **Phase 4:** Evolution System & Pet Stages
- ✅ **Phase 5:** Multiple Pets & Statistics Dashboard
- ✅ **Phase 6:** REST API & JSON Interface
- ✅ **Phase 7A:** XP System Foundation (Player progression, XP earning, multipliers)
- ✅ **Phase 7B:** Equipment System (Equippable items, stat modifiers, mourning mechanics)

### Upcoming Phases 🚧

**Phase 7: Progression & Equipment System** (Idle/Incremental Mechanics)
- 7A: XP System Foundation ✅ COMPLETED
- 7B: Equipment System ✅ COMPLETED
- 7C: XP Shop (1-2 sessions)
- 7D: Consumables & Auto-Actions (2-3 sessions)
- 7E: Power-Leveling & Balance Tuning (1 session)

**Phase 8: Achievements & Mini-Games** (2-3 sessions)

**Phase 9: Prestige System & Advanced Polish** (2-3 sessions)

---

## Phase 7: Progression & Equipment System

### Phase 7A: XP System Foundation ✅ COMPLETED
**Duration:** 1 Claude Code session
**Goal:** Implement XP earning and tracking without spending mechanics

**Completed Deliverables:**
- ✅ PlayerProgression aggregate (playerId, totalXP, lifetimeXP, totalPetsCreated)
- ✅ XP earning from interactions (feed +10, play +15, clean +10) via XPEarningSaga
- ✅ Pet XP multiplier field in Pet aggregate and PetStatusView (starts 1.0x)
- ✅ TimePassedEvent extended with xpMultiplierChange and newXpMultiplier
- ✅ XPEarningSaga coordinates XP earning across aggregates
- ✅ PlayerProgressionProjection and PlayerProgressionView for queries
- ✅ PlayerInitializationService grants 100 starting XP
- ✅ Dashboard integration shows player progression
- ✅ REST endpoint: `GET /api/player/progression`
- ✅ All tests passing (120 tests, 0 failures)

**Implementation Notes:**
- XP multiplier mechanics ready for future milestone-based increases
- Survival and evolution XP bonuses deferred to Phase 7B for equipment balance
- PetCreatedForPlayerEvent added but handler deferred (saga warns, no errors)
- Clean separation: XPEarningSaga handles earning, PlayerProgression handles state

---

### Phase 7B: Equipment System ✅ COMPLETED
**Duration:** 1 Claude Code session
**Goal:** Introduce equippable items that modify pet stats

**Completed Deliverables:**
- ✅ PlayerInventory aggregate (aggregate ID: "PLAYER_INVENTORY")
- ✅ Pet aggregate updates: equippedItems Map, maxEquipmentSlots field
- ✅ Equipment domain model: EquipmentSlot, StatModifier, EquipmentItem
- ✅ Equipment slots: FOOD_BOWL, TOY, ACCESSORY
- ✅ Slots unlock by stage: Baby=1, Teen=2, Adult=3, Egg=0
- ✅ Starter package: Basic Bowl (+10% food efficiency), Simple Toy (+10% play efficiency), Comfort Blanket (+1 health/tick)
- ✅ 6 stat modifiers: HUNGER_DECAY_RATE, HAPPINESS_DECAY_RATE, HEALTH_DECAY_RATE, FOOD_EFFICIENCY, PLAY_EFFICIENCY, HEALTH_REGEN
- ✅ EquipmentSaga: cross-aggregate coordination for equip/unequip
- ✅ PetDeathSaga: returns equipped items to inventory on pet death
- ✅ Pet mourning mechanics: alive pets lose 10% happiness when another pet dies
- ✅ InventoryProjection with GetInventoryQuery and GetInventoryItemQuery
- ✅ PetStatusView updated with equippedItems and maxEquipmentSlots
- ✅ Equipment modifiers applied to all pet actions (feed, play, time ticks)
- ✅ All tests passing (120 tests, 0 failures)

**Implementation Notes:**
- Used separate aggregate ID for PlayerInventory to avoid Axon ID conflicts
- Equipment data stored as JSON in TEXT columns for H2 compatibility
- Fixed Axon XStream serialization by using mutable collections (ArrayList, HashMap)
- PetDiedEvent extended to include equipped items list
- Starter equipment initialized with PlayerInitializationService
- CLI and REST endpoints deferred to later in Phase 7B or Phase 7C

**Testing Completed:**
- ✅ Unit tests for Pet aggregate with equipment commands
- ✅ Unit tests for PlayerInventory aggregate
- ✅ Integration tests verify equipment initialization
- ✅ All existing tests updated for new PetDiedEvent signature

---

### Phase 7C: XP Shop
**Duration:** 1-2 Claude Code sessions
**Goal:** Allow spending XP to purchase equipment and permanent upgrades

**Key Deliverables:**
- ItemDefinition entity (catalog with XP costs)
- Equipment items for sale (9 items):
  - Food Bowl: Slow Feeder (200 XP), Nutrient Bowl (300 XP), Auto-Feeder (500 XP)
  - Toy: Toy Box (200 XP), Exercise Wheel (300 XP), Entertainment System (500 XP)
  - Accessory: Cozy Bed (200 XP), Health Monitor (400 XP), XP Charm (600 XP)
- Permanent upgrades (8 upgrades):
  - Efficient Metabolism (200 XP), Happy Disposition (150 XP), Sturdy Genetics (150 XP)
  - Industrial Kitchen (500 XP), Fast Hatcher (250 XP)
  - Multi-Pet License I/II/III (300/600/1000 XP)
- Purchase flow saga with XP validation
- Create pet XP cost: FREE for 1st, then 50/100/150... (increases by 50)
- Starter package: 100 XP, 3 Apples, 1 Ball, Comfort Blanket
- CLI: `shop`, `buy <itemType>`, `upgrades`
- REST endpoints: `GET /api/shop/items`, `GET /api/shop/upgrades`, `POST /api/shop/purchase/{type}/{id}`

**Testing:**
- Purchase item, verify XP deducted
- Insufficient XP, verify rejection
- Purchase permanent upgrade, verify applies to new pets
- Create multiple pets, verify cost increases
- Multi-Pet License, verify can create more pets

---

### Phase 7D: Consumables & Auto-Actions
**Duration:** 2-3 Claude Code sessions
**Goal:** Introduce consumable items and automation mechanics

**Key Deliverables:**
- Consumable items (7 types):
  - Food: Apple (50 XP), Pizza (100 XP), Gourmet Meal (200 XP)
  - Medicine: Basic (100 XP), Advanced (200 XP)
  - Treats: Cookie (75 XP), Premium Toy (150 XP)
- UseConsumableCommand with inventory check saga
- Auto-action logic in TimeTickCommand:
  - Auto-Feeder triggers at hunger >70
  - Entertainment System triggers at happiness <50
  - Both query inventory, consume items if available
- Enhanced sickness mechanic:
  - Health <30 for 3+ ticks: 20% chance/tick of PetBecameSickEvent
  - Sickness: hunger/happiness decay +50%, cannot play
  - Advanced Medicine cures, Basic Medicine doesn't
- Apply equipment modifiers to consumable effectiveness (Nutrient Bowl, Industrial Kitchen)
- CLI: `use <petId> <consumableType>`, `craft <consumableType> <qty>`
- REST endpoint: `POST /api/pets/{id}/use`

**Testing:**
- Use consumable, verify inventory decreases
- Auto-Feeder triggers when conditions met
- Auto-Feeder silent when no consumables
- Pet becomes sick at low health
- Advanced medicine cures sickness
- Equipment modifiers affect consumables

---

### Phase 7E: Power-Leveling & Balance Tuning
**Duration:** 1 Claude Code session
**Goal:** Fine-tune progression curves and test multi-pet strategies

**Key Deliverables:**
- XP multiplier cap at 5.0x (prevent infinite scaling)
- Multiplier decay: -0.05x per 10 ticks if stats <50
- Dashboard enhancements: XP/min rate, highest multiplier, XP spent vs earned
- CLI polish: color-code equipment by cost tier, show recommendations
- REST endpoint: `GET /api/analytics/xp-rate`
- Balance testing scenarios:
  - Early game: survive to adult with starter package
  - Mid game: afford Auto-Feeder by age 100
  - Late game: maintain 3+ pets with power-leveling

**Testing:**
- Verify multiplier caps at 5.0x
- Test death penalty affects other pets
- Validate early/mid/late game progression feels achievable
- Multi-pet XP farming strategies work as intended

---

## Phase 8: Achievements & Mini-Games

**Duration:** 2-3 Claude Code sessions
**Goal:** Track progression through achievements, add mini-games for XP bonuses

**Key Deliverables:**
- Extend PlayerProgression aggregate with achievements set
- 12 achievement types across 4 categories (care, progression, equipment, mastery)
- Each achievement grants one-time XP bonus (100-1000 XP)
- Achievement tracking saga monitors all events
- 3 mini-games:
  - Guess Game: 5 tick cooldown, success +30 XP, failure +10 XP
  - Reflex Game: 5 tick cooldown, fast +50 XP, medium +30 XP, slow +15 XP
  - Emotional Intelligence: 10 tick cooldown, correct +40 XP, wrong +10 XP
- Pet aggregate tracks lastPlayedGames for cooldown enforcement
- All mini-game XP affected by pet's multiplier
- CLI: `achievements`, `game guess/reflex/emotional <petId>`
- REST endpoints: `GET /api/achievements`, `POST /api/games/{petId}/{gameType}`

**Testing:**
- Unlock achievement, verify XP bonus
- Play mini-game, verify XP earned with multiplier
- Attempt during cooldown, verify rejection
- Achievement progress tracked across pets
- Notifications display on unlock

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
- Admin tools: reset projection, grant XP, force prestige
- CLI: `admin reset-projection <name>`, `admin grant-xp <amount>`, `admin prestige-player`
- REST endpoints: `POST /api/admin/reset-projection/{name}`, `POST /api/admin/grant-xp`

**Testing:**
- Prestige resets correctly, bonuses apply
- Snapshots reduce event replay time
- Load test with 100+ pets stable
- Health checks report correctly
- Admin tools work as expected

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
6. **Document:** Update DESIGN.md with implementation notes
7. **Validate:** Manual testing of full flow via CLI + API
8. **Commit:** Git commit with descriptive message

### Quality Standards
- All new code passes Checkstyle + SpotBugs
- Test coverage >80% for new code (JaCoCo)
- All REST endpoints documented in Swagger
- CLI help text updated for new commands
- Event sourcing tested (command → event → projection flow)
- Saga coordination tested for cross-aggregate operations

---

## Estimated Timeline

**Total Duration:** ~15-20 Claude Code sessions

- Phase 7A: 1-2 sessions
- Phase 7B: 2-3 sessions
- Phase 7C: 1-2 sessions
- Phase 7D: 2-3 sessions
- Phase 7E: 1 session
- Phase 8: 2-3 sessions
- Phase 9: 2-3 sessions

**Velocity Notes:**
- Sessions with simple aggregates (PlayerProgression) faster
- Sessions with complex sagas (equip/unequip coordination) slower
- Balance tuning (Phase 7E) highly iterative, may extend
- Each phase builds on previous, maintain quality to avoid rework

---

## Success Metrics

### Phase 7 Success
- [x] Player can earn XP from interactions (Phase 7A complete)
- [ ] XP multiplier increases with pet age/care (infrastructure ready, Phase 7B)
- [x] Equipment modifies pet stats with trade-offs (Phase 7B complete)
- [ ] Can purchase items with XP (Phase 7C)
- [ ] Automation items reduce manual actions (Phase 7D)
- [ ] Multi-pet power-leveling strategies viable (Phase 7E)
- [x] Player progression tracked and persisted (Phase 7A complete)

### Phase 8 Success
- [ ] Achievements unlock with XP bonuses
- [ ] Mini-games provide alternative XP source
- [ ] Cooldowns prevent mini-game spam
- [ ] Achievement progress visible across pets

### Phase 9 Success
- [ ] Prestige system functional
- [ ] Prestige bonuses apply on reset
- [ ] Snapshots improve load time for old pets
- [ ] Load test: 100+ pets stable
- [ ] Production Docker build works

---

## Notes

- **Design Flexibility:** Balance numbers (XP costs, stat modifiers, multipliers) should be easy to tune without code changes (config files or database)
- **Event Sourcing Benefits:** Can replay events to test balance changes retroactively
- **Saga Complexity:** Most complexity in Phase 7B/7D with cross-aggregate coordination
- **Frontend Readiness:** REST API designed for Next.js frontend (CORS, async, JSON)
- **Axon Patterns:** Heavy use of sagas for coordination, projections for queries, snapshots for performance
