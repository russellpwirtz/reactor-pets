# Reactor Pets - Implementation Roadmap

**Last Updated:** 2025-10-29
**Current Phase:** Phase 7 Complete → Phase 7E (Power-Leveling) Next

---

## Quick Reference

### Completed Phases ✅
- ✅ **Phase 1-6:** Foundation, lifecycle, time system, evolution, multiple pets, REST API
- ✅ **Phase 7A:** XP System Foundation
- ✅ **Phase 7B:** Equipment System
- ✅ **Phase 7C:** XP Shop System
- ✅ **Phase 7D:** Consumables & Auto-Actions

### Current Phase 🚧
**Ready for Phase 7E: Power-Leveling & Balance Tuning**

### Upcoming Phases 📋
- **Phase 7E:** Power-Leveling & Balance Tuning (1 session)
- **Phase 8:** Achievements & Mini-Games (2-3 sessions)
- **Phase 9:** Prestige System & Advanced Polish (2-3 sessions)

---

## Completed Phase 7: XP & Equipment System ✅

### Phase 7A: XP System Foundation ✅
- Player progression tracking with XP, levels, and lifetime stats
- XP earning from pet interactions (feed, play, clean)
- XP multiplier based on pet stats
- REST endpoints for progression tracking

### Phase 7B: Equipment System ✅
- Equipment items with slots (Food Bowl, Toy, Accessory)
- Equipment modifiers affecting pet stats
- Inventory management and equip/unequip mechanics
- EquipmentSaga for coordinating equipment changes
- REST endpoints for equipment management

### Phase 7C: XP Shop ✅
- Shop catalog with equipment and permanent upgrades
- XP-based purchasing system
- ShopPurchaseSaga with XP validation
- Multi-pet license progression
- REST endpoints for shop browsing and purchasing

### Phase 7D: Consumables & Auto-Actions ✅
- 7 consumable types (food, medicine, treats)
- Consumable usage with equipment modifier effects
- Pet sickness mechanic (triggered by low health)
- Auto-action equipment (Auto-Feeder, Entertainment System)
- ConsumableUsageSaga for inventory coordination
- Integrated consumables into shop system
- **Saga Architecture Refactoring** ✅ (2025-10-29)
  - Fixed all 3 sagas to follow proper event-driven architecture
  - ShopPurchaseSaga, EquipmentSaga, ConsumableUsageSaga now listen to events instead of handling commands
  - Aggregates properly handle commands and emit events
  - Cross-aggregate coordination via sagas
  - All 185 tests passing

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

### Phase 7 Success ✅
- [x] Player can earn XP from interactions (7A)
- [x] Equipment modifies pet stats with trade-offs (7B)
- [x] Can purchase equipment and upgrades with XP (7C)
- [x] Player progression tracked and persisted (7A)
- [x] Consumables provide immediate benefits (7D)
- [x] Automation items reduce manual actions (7D)
- [ ] Multi-pet power-leveling strategies viable (7E - next phase)

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

**Phase 7 Complete - Key Patterns:**
- **Saga Coordination:** ShopPurchaseSaga, EquipmentSaga, ConsumableUsageSaga handle cross-aggregate operations
- **Static Catalogs:** ShopCatalog, EquipmentCatalog, ConsumableCatalog provide game data (query-only, no aggregates)
- **Quantity vs Instance:** Consumables are quantity-based, equipment is instance-based
- **Equipment Modifiers:** FOOD_EFFICIENCY, PLAY_EFFICIENCY, XP_BOOST etc. affect various game mechanics
- **Starter Package:** New players receive initial equipment and consumables (3 Apples, Cozy Bed)
- **Pet Creation Costs:** First pet FREE, then 50/100/150 XP (requires Multi-Pet Licenses)

**Axon Patterns:**
- Heavy use of sagas for coordination (Shop, Equipment, Consumables)
- Projections for queries (read models)
- Static catalogs for game data (Shop, Equipment, Consumables)
