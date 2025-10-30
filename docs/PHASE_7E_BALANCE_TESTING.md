# Phase 7E: Balance Testing & Validation

**Date:** 2025-10-29
**Phase:** 7E - Power-Leveling & Balance Tuning

## Overview

This document outlines manual balance testing scenarios to validate the Phase 7E progression curve and ensure the game loop is enjoyable.

---

## Testing Scenarios

### Scenario 1: Early Game Survival (0-30 minutes)
**Goal:** Verify a new player can survive their first pet to adult stage with the starter package.

**Starter Package:**
- 100 starting XP
- 3 Apples (consumable)
- 1 Cozy Bed (equipment)

**Test Steps:**
1. Create a new pet with `POST /api/pets` (name: "TestPet1", type: "DOG")
2. Verify pet starts with:
   - Hunger: 30
   - Happiness: 70
   - Health: 100
   - XP Multiplier: 1.0x
3. Monitor pet for 50 ticks (EGG → BABY transition at age 5)
   - Feed pet when hunger > 70 (use Apples strategically)
   - Play with pet when happiness < 50
   - Clean pet when health < 80
4. Continue monitoring until BABY → TEEN (age 20)
5. Track XP earned and verify player can afford first upgrade by age 20

**Expected Outcomes:**
- [ ] Pet survives to BABY stage (age 5) without dying
- [ ] Player earns at least 200 XP by age 20 (enough for first equipment purchase)
- [ ] XP multiplier reaches ~1.2x by age 20 (care quality bonus)
- [ ] Starter package consumables last until first purchase

**Success Criteria:**
✅ Pet reaches BABY stage without death
✅ Player can afford first equipment item (200 XP) by age 20
✅ Gameplay feels manageable but engaging

---

### Scenario 2: Mid-Game Automation (30-60 minutes)
**Goal:** Verify player can afford Auto-Feeder by age 100 and automation reduces manual actions.

**Prerequisites:**
- Pet at age 50+ (TEEN → ADULT transition)
- Player has purchased 1-2 basic equipment items

**Test Steps:**
1. Continue from Scenario 1 or create new pet at TEEN stage
2. Track XP earning rate over 50 ticks
3. Purchase "Auto-Feeder" (500 XP) when available
4. Equip Auto-Feeder to FOOD_BOWL slot
5. Verify auto-feeding triggers when hunger > 70
6. Monitor pet for 50 ticks with minimal manual intervention

**Expected Outcomes:**
- [ ] Player earns 500 XP by age 100 (for Auto-Feeder purchase)
- [ ] XP multiplier reaches ~2.0x by age 100 (age milestones + care quality)
- [ ] Auto-Feeder reduces manual feeding by ~70%
- [ ] Player has breathing room to step away for short periods

**Success Criteria:**
✅ Auto-Feeder purchased by age 100
✅ Manual actions reduced significantly
✅ Progression feels rewarding

---

### Scenario 3: Late-Game Power-Leveling (60+ minutes)
**Goal:** Verify player can maintain 3+ pets simultaneously using power-leveling strategies.

**Prerequisites:**
- Player has purchased Multi-Pet License II (can own 3 pets)
- Player has 1 adult pet with high XP multiplier (3.0x+)

**Test Steps:**
1. Create 2 additional pets (total 3 alive pets)
2. Focus care on the pet with highest multiplier
3. Track XP earning rate across all pets
4. Verify older pets earn more XP per interaction
5. Use XP from older pets to fund younger pets' equipment
6. Monitor for 100 ticks with 3 active pets

**Expected Outcomes:**
- [ ] Oldest pet maintains 3.0x+ multiplier
- [ ] Newer pets benefit from purchased upgrades
- [ ] Total XP earning rate is 3-5x higher than single pet
- [ ] Player can manage 3 pets without constant attention

**Success Criteria:**
✅ 3 pets alive simultaneously
✅ XP earning rate is significantly higher than single pet
✅ Multi-pet management feels strategic, not overwhelming

---

## Balance Validation Checks

### XP Multiplier Mechanics
- [ ] Multiplier increases by +0.1x every 50 ticks (age milestones)
- [ ] Multiplier increases by +0.05x when all stats >70 (care quality)
- [ ] Multiplier caps at 5.0x (prevent infinite scaling)
- [ ] Multiplier decays by -0.05x per 10 ticks when any stat <50
- [ ] Decay does not reduce multiplier below 1.0x

### Equipment Impact
- [ ] "Slow Feeder" reduces hunger decay by 40% (noticeable difference)
- [ ] "Auto-Feeder" triggers when hunger > 70 (consumes Apple from inventory)
- [ ] "Entertainment System" triggers when happiness < 50
- [ ] Equipment modifiers apply correctly to consumable effects

### XP Earning Rates
- [ ] Feed action grants 10 XP (base) × multiplier
- [ ] Play action grants 15 XP (base) × multiplier
- [ ] Clean action grants 10 XP (base) × multiplier
- [ ] Survival XP grants 1 XP per tick × multiplier
- [ ] Evolution grants bonus XP (50/100/200 for stage transitions)

### Progression Pacing
- [ ] First 10 minutes: constant babysitting, learn mechanics
- [ ] 10-30 minutes: first upgrades, breathing room
- [ ] 30-60 minutes: multi-pet management begins
- [ ] 60+ minutes: strategic builds, power-leveling optimization

---

## API Endpoints for Testing

### Create Pet
```bash
curl -X POST http://localhost:8080/api/pets \
  -H "Content-Type: application/json" \
  -d '{"name": "TestPet1", "type": "DOG"}'
```

### Get Player Progression
```bash
curl http://localhost:8080/api/progression
```

### Get XP Analytics (Phase 7E)
```bash
curl http://localhost:8080/api/analytics/xp-rate
```

### Purchase Equipment
```bash
curl -X POST http://localhost:8080/api/shop/purchase/equipment/AUTO_FEEDER
```

### Equip Item
```bash
curl -X POST http://localhost:8080/api/pets/{petId}/equipment/equip \
  -H "Content-Type: application/json" \
  -d '{"itemId": "auto-feeder-001", "slot": "FOOD_BOWL"}'
```

### Feed Pet
```bash
curl -X POST http://localhost:8080/api/pets/{petId}/feed
```

---

## Known Issues & Observations

### Phase 7E Improvements Made:
- ✅ XP multiplier now caps at 5.0x (prevents infinite scaling)
- ✅ Multiplier decay added (-0.05x per 10 ticks if stats <50)
- ✅ Analytics tracking: totalXPSpent, highestXPMultiplier
- ✅ New endpoint: `/api/analytics/xp-rate`

### Pending Enhancements:
- ⏳ XP per minute calculation (requires time-window tracking)
- ⏳ Equipment recommendations based on pet stats
- ⏳ Dashboard UI enhancements for analytics display

---

## Manual Testing Checklist

### Phase 7E Specific Tests
- [ ] Create pet, verify XP multiplier starts at 1.0x
- [ ] Wait 50 ticks, verify multiplier increases to 1.1x
- [ ] Maintain high stats (>70), verify care quality bonus (+0.05x)
- [ ] Let stats drop <50, verify decay triggers after 10 ticks
- [ ] Let multiplier grow to 5.0x, verify cap enforcement
- [ ] Check `/api/analytics/xp-rate` returns correct totals
- [ ] Verify totalXPSpent updates when purchasing items
- [ ] Verify highestXPMultiplier tracks across all pets

### Integration Tests
- [ ] All 185+ tests pass
- [ ] No Checkstyle violations
- [ ] No SpotBugs warnings
- [ ] Swagger UI shows new `/api/analytics/xp-rate` endpoint

---

## Success Metrics

### Early Game (0-30 min):
✅ Pet survives to BABY without dying
✅ Player earns 200+ XP by age 20
✅ Gameplay feels engaging, not frustrating

### Mid Game (30-60 min):
✅ Auto-Feeder affordable by age 100
✅ Manual actions reduced by ~70%
✅ Progression feels rewarding

### Late Game (60+ min):
✅ 3 pets manageable simultaneously
✅ XP rate 3-5x higher than single pet
✅ Strategic depth without overwhelming complexity

---

## Conclusion

Phase 7E establishes the foundation for a balanced idle/incremental progression curve. The XP multiplier cap and decay prevent runaway scaling while maintaining strategic depth. Future phases (8-9) will build on these mechanics with achievements, mini-games, and prestige systems.
