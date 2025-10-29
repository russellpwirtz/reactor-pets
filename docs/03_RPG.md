# Virtual Pet - Tactical RPG Progression System
## Care-to-Combat Gameplay Addendum

**Prerequisites:** This document assumes you have completed the main design document (Phases 1-9) and have a working pet system with event sourcing, projections, time-based degradation, and basic gameplay mechanics.

---

## Overview

This addendum transforms the virtual pet from a care simulator into a **tactical RPG with persistent care mechanics**, where daily pet maintenance generates experience points that fuel character progression and enable dungeon exploration. This creates a compelling gameplay loop:

**Care for Pet → Earn XP → Level Up → Allocate Skills → Test Build in Dungeon → Earn Rewards → Repeat**

### Core Design Philosophy

- **Care is rewarded, not just required:** Every interaction generates XP
- **Builds create replayability:** Different skill allocations lead to different strategies
- **Dungeons test optimization:** Combat reveals strengths/weaknesses of your build
- **Meaningful choices:** Where you spend skill points determines your pet's identity
- **Long-term progression:** Months of play to fully explore all skill trees

---

## Architecture: Combat & Progression Contexts

### New Bounded Contexts

```
┌────────────────────────────────────────────────────────────┐
│              Pet Progression Context                       │
│                                                             │
│  ┌──────────────────┐      ┌────────────────────────┐    │
│  │  PetProgression  │      │  Care Reward Saga       │    │
│  │   Aggregate      │      │  (Awards XP)            │    │
│  │  - Level         │      └────────────────────────┘    │
│  │  - XP            │                                      │
│  │  - Skills        │      ┌────────────────────────┐    │
│  │  - Skill Points  │      │  Progression Projection │    │
│  └──────────────────┘      │  (Level, skills, stats) │    │
│                             └────────────────────────┘    │
└────────────────────────────────────────────────────────────┘
         │                             │
         ▼                             ▼
┌────────────────────────────────────────────────────────────┐
│              Dungeon Combat Context                        │
│                                                             │
│  ┌──────────────────┐      ┌────────────────────────┐    │
│  │   DungeonRun     │      │  Combat Resolution      │    │
│  │   Aggregate      │      │  Saga                   │    │
│  │  - Floor         │      │  (Turn coordination)    │    │
│  │  - Enemies       │      └────────────────────────┘    │
│  │  - Combat State  │                                      │
│  └──────────────────┘      ┌────────────────────────┐    │
│                             │  Dungeon Projection     │    │
│                             │  (Active runs, history) │    │
│                             └────────────────────────┘    │
└────────────────────────────────────────────────────────────┘
         │                             │
         ▼                             ▼
    Pet Context              Inventory Context
  (Feeds stats)              (Equipment, items)
```

---

## Phase 15: XP & Leveling Foundation

**Goal:** Implement experience point system that rewards all care activities, with level-up mechanics and skill point distribution.

### Deliverables

1. **PetProgression Aggregate**
   ```java
   @Aggregate
   class PetProgression {
     @AggregateIdentifier
     private String petId;
     private int level;
     private int currentXP;
     private int totalXP; // Lifetime XP earned
     private int unspentSkillPoints;
     private Map<Skill, Integer> allocatedSkills; // FLAME_BREATH_I: 2
     private SkillTree primaryTree; // FIRE, SCALES, AGILITY, HYBRID
     private Instant lastXPAward; // For rate limiting
     
     @CommandHandler
     public PetProgression(CreateProgressionCommand cmd) {
       apply(new ProgressionCreatedEvent(cmd.petId(), 1, 0, 0));
     }
     
     @CommandHandler
     void handle(AwardXPCommand cmd) {
       // Validate: pet is alive, not rate-limited
       if (!canAwardXP(cmd)) {
         throw new IllegalStateException("Cannot award XP: " + cmd.reason());
       }
       
       int newXP = this.currentXP + cmd.amount();
       int xpNeeded = xpForNextLevel(this.level);
       
       if (newXP >= xpNeeded) {
         // Level up!
         int overflow = newXP - xpNeeded;
         apply(new PetLeveledUpEvent(petId, level, level + 1, 3, cmd.timestamp()));
         apply(new XPAwardedEvent(petId, overflow, totalXP + cmd.amount(), cmd.reason()));
       } else {
         apply(new XPAwardedEvent(petId, newXP, totalXP + cmd.amount(), cmd.reason()));
       }
     }
     
     @EventSourcingHandler
     void on(PetLeveledUpEvent event) {
       this.level = event.newLevel();
       this.unspentSkillPoints += event.skillPointsAwarded();
       this.currentXP = 0; // Reset to 0 after level up
     }
     
     @EventSourcingHandler
     void on(XPAwardedEvent event) {
       this.currentXP = event.newCurrentXP();
       this.totalXP = event.newTotalXP();
       this.lastXPAward = event.timestamp();
     }
     
     private int xpForNextLevel(int level) {
       // Exponential curve: level 2 needs 100, level 10 needs 2000
       return (int) (100 * Math.pow(1.2, level - 1));
     }
   }
   ```

2. **XP Reward Configuration**
   ```java
   enum CareAction {
     FEED(10, "Fed when hungry"),
     PLAY(15, "Played when bored"),
     CLEAN(12, "Cleaned pet"),
     MEDICINE(20, "Gave medicine proactively"),
     PERFECT_DAY(50, "All needs met for 24 hours"),
     EMOTIONAL_GAME_SUCCESS(30, "Correctly interpreted pet's needs"),
     EMOTIONAL_GAME_FAIL(5, "Attempted emotional game"),
     PET_CHAT(8, "Had conversation with pet"),
     ITEM_USED(5, "Used special item");
     
     private final int baseXP;
     private final String description;
   }
   ```

3. **Care Reward Saga**
   ```java
   @Saga
   class CareRewardSaga {
     
     @SagaEventHandler(associationProperty = "petId")
     void on(PetFedEvent event) {
       // Query pet status to determine if feeding was "needed"
       CompletableFuture<PetStatus> statusFuture = 
         queryGateway.query(new GetPetStatusQuery(event.petId()), PetStatus.class);
       
       statusFuture.thenAccept(status -> {
         int xpAmount = calculateFeedingXP(event, status);
         commandGateway.send(new AwardXPCommand(
           event.petId(), 
           xpAmount, 
           CareAction.FEED.name(),
           event.timestamp()
         ));
       });
     }
     
     @SagaEventHandler(associationProperty = "petId")
     void on(PetPlayedWithEvent event) {
       commandGateway.send(new AwardXPCommand(
         event.petId(), 
         CareAction.PLAY.baseXP, 
         CareAction.PLAY.name(),
         event.timestamp()
       ));
     }
     
     @SagaEventHandler(associationProperty = "petId")
     void on(PetCleanedEvent event) {
       commandGateway.send(new AwardXPCommand(
         event.petId(), 
         CareAction.CLEAN.baseXP, 
         CareAction.CLEAN.name(),
         event.timestamp()
       ));
     }
     
     @SagaEventHandler(associationProperty = "petId")
     void on(PetMedicatedEvent event) {
       // Higher XP if medicine was given proactively (not already sick)
       PetStatus status = queryGateway.query(
         new GetPetStatusQuery(event.petId()), 
         PetStatus.class
       ).join();
       
       int xpAmount = status.isSick() ? 10 : CareAction.MEDICINE.baseXP;
       commandGateway.send(new AwardXPCommand(
         event.petId(), xpAmount, CareAction.MEDICINE.name(), event.timestamp()
       ));
     }
     
     @SagaEventHandler(associationProperty = "petId")
     void on(EmotionalGameResolvedEvent event) {
       CareAction action = event.correct() ? 
         CareAction.EMOTIONAL_GAME_SUCCESS : 
         CareAction.EMOTIONAL_GAME_FAIL;
       
       commandGateway.send(new AwardXPCommand(
         event.petId(), action.baseXP, action.name(), event.timestamp()
       ));
     }
     
     @SagaEventHandler(associationProperty = "petId")
     void on(PetRespondedEvent event) {
       // Award XP for chatting with pet (encourages engagement)
       commandGateway.send(new AwardXPCommand(
         event.petId(), 
         CareAction.PET_CHAT.baseXP, 
         CareAction.PET_CHAT.name(),
         event.timestamp()
       ));
     }
     
     private int calculateFeedingXP(PetFedEvent event, PetStatus status) {
       // Bonus XP for feeding when hunger is high
       if (status.hunger() > 70) {
         return CareAction.FEED.baseXP + 5; // Bonus for good timing
       } else if (status.hunger() < 30) {
         return CareAction.FEED.baseXP / 2; // Reduced XP for overfeeding
       }
       return CareAction.FEED.baseXP;
     }
   }
   ```

4. **Perfect Day Detection**
   ```java
   @Component
   class PerfectDayDetector {
     // Tracks pet stats over 24 ticks (24 hours in game time)
     private final Map<String, Queue<StatSnapshot>> recentStats = new ConcurrentHashMap<>();
     
     @EventHandler
     void on(TimePassedEvent event) {
       Queue<StatSnapshot> history = recentStats.computeIfAbsent(
         event.petId(), 
         k -> new LinkedList<>()
       );
       
       // Query current stats
       PetStatus status = queryGateway.query(
         new GetPetStatusQuery(event.petId()), 
         PetStatus.class
       ).join();
       
       history.add(new StatSnapshot(status, event.timestamp()));
       
       // Keep only last 24 ticks
       while (history.size() > 24) {
         history.poll();
       }
       
       // Check if all 24 ticks had good stats
       if (history.size() == 24 && isPerfectDay(history)) {
         commandGateway.send(new AwardXPCommand(
           event.petId(),
           CareAction.PERFECT_DAY.baseXP,
           CareAction.PERFECT_DAY.name(),
           event.timestamp()
         ));
       }
     }
     
     private boolean isPerfectDay(Queue<StatSnapshot> history) {
       return history.stream().allMatch(snapshot -> 
         snapshot.hunger() < 70 && 
         snapshot.happiness() > 40 && 
         snapshot.health() > 50
       );
     }
   }
   ```

5. **Progression Projection**
   ```java
   @Component
   @ProcessingGroup("pet-progression")
   class ProgressionProjection {
     
     @EventHandler
     void on(ProgressionCreatedEvent event) {
       ProgressionView view = new ProgressionView(
         event.petId(),
         1, // Starting level
         0, // Starting XP
         0, // Starting total XP
         0, // No skill points yet
         new HashMap<>()
       );
       repository.save(view);
     }
     
     @EventHandler
     void on(XPAwardedEvent event) {
       ProgressionView view = repository.findById(event.petId()).orElseThrow();
       view.setCurrentXP(event.newCurrentXP());
       view.setTotalXP(event.newTotalXP());
       view.setLastUpdated(event.timestamp());
       repository.save(view);
     }
     
     @EventHandler
     void on(PetLeveledUpEvent event) {
       ProgressionView view = repository.findById(event.petId()).orElseThrow();
       view.setLevel(event.newLevel());
       view.setCurrentXP(0);
       view.setUnspentSkillPoints(view.getUnspentSkillPoints() + event.skillPointsAwarded());
       view.setLastUpdated(event.timestamp());
       repository.save(view);
     }
     
     @QueryHandler
     ProgressionView handle(GetProgressionQuery query) {
       return repository.findById(query.petId()).orElseThrow();
     }
     
     @QueryHandler
     List<ProgressionView> handle(GetLeaderboardQuery query) {
       return repository.findAll()
         .stream()
         .sorted(Comparator.comparing(ProgressionView::getLevel)
                           .thenComparing(ProgressionView::getTotalXP)
                           .reversed())
         .limit(query.limit())
         .toList();
     }
   }
   ```

6. **CLI Commands**
   ```bash
   > xp <petId>
   ═══════════════════════════════════════
   Fluffy's Progression
   ═══════════════════════════════════════
   Level: 5
   XP: 347 / 500 (69% to next level)
   Total XP Earned: 1,847
   Unspent Skill Points: 3
   
   Recent XP Gains:
   - Fed when hungry: +10 XP (2 minutes ago)
   - Played with pet: +15 XP (15 minutes ago)
   - Emotional game success: +30 XP (1 hour ago)
   
   > level <petId>
   Fluffy leveled up to 5!
   +3 skill points awarded
   Use 'skills <petId>' to view skill tree
   
   > leaderboard
   ═══════════════════════════════════════
   Top 10 Pets by Level
   ═══════════════════════════════════════
   1. Drago (Dragon, Level 12, 5,492 XP)
   2. Whiskers (Cat, Level 10, 3,891 XP)
   3. Fluffy (Dragon, Level 5, 1,847 XP)
   ...
   ```

7. **Rate Limiting**
   ```java
   // Prevent XP farming by limiting awards
   private static final Duration XP_COOLDOWN = Duration.ofSeconds(5);
   
   boolean canAwardXP(AwardXPCommand cmd) {
     if (lastXPAward == null) return true;
     return Duration.between(lastXPAward, cmd.timestamp()).compareTo(XP_COOLDOWN) > 0;
   }
   ```

### Technical Notes
- XP curve is exponential to create long-term progression
- Perfect day bonus encourages consistent care over power-leveling
- Rate limiting prevents spam-feeding for XP
- Progression aggregate is separate from Pet aggregate (different concerns)
- Saga ensures XP is awarded for all care actions automatically

---

## Phase 16: Skill Trees & Allocation

**Goal:** Define skill trees with prerequisites, implement skill point allocation, and create tech tree visualization.

### Deliverables

1. **Skill Definition System**
   ```java
   enum SkillTree {
     FIRE,    // Offensive, high damage
     SCALES,  // Defensive, tanking
     AGILITY, // Utility, tactical
     HYBRID   // Jack of all trades
   }
   
   enum Skill {
     // FIRE TREE
     FLAME_BREATH_I(SkillTree.FIRE, 1, null, 1, 
                    "Basic fire attack, +10 damage"),
     FLAME_BREATH_II(SkillTree.FIRE, 3, FLAME_BREATH_I, 2, 
                     "Improved fire attack, +25 damage, applies burn"),
     INFERNO(SkillTree.FIRE, 5, FLAME_BREATH_II, 3, 
             "Massive AoE fire attack, +40 damage to all enemies"),
     DRAGONS_FURY(SkillTree.FIRE, 10, INFERNO, 5, 
                  "Ultimate: Triple damage for 3 turns"),
     
     // SCALES TREE
     HARDENED_SCALES_I(SkillTree.SCALES, 1, null, 1, 
                       "Increase defense by +15"),
     HARDENED_SCALES_II(SkillTree.SCALES, 3, HARDENED_SCALES_I, 2, 
                        "Increase defense by +30"),
     REGENERATION(SkillTree.SCALES, 4, HARDENED_SCALES_I, 2, 
                  "Heal 5 HP per turn in combat"),
     IRON_HIDE(SkillTree.SCALES, 6, HARDENED_SCALES_II, 3, 
               "Defense +50, reflect 10% damage"),
     IMMORTAL_SHELL(SkillTree.SCALES, 10, IRON_HIDE, 5, 
                    "Ultimate: Immune to damage for 3 turns"),
     
     // AGILITY TREE
     QUICK_REFLEXES(SkillTree.AGILITY, 1, null, 1, 
                    "+15% dodge chance"),
     DOUBLE_STRIKE(SkillTree.AGILITY, 3, QUICK_REFLEXES, 2, 
                   "Attack twice in one turn"),
     PHASE_SHIFT(SkillTree.AGILITY, 5, QUICK_REFLEXES, 2, 
                 "Teleport, dodge next attack"),
     CRITICAL_HIT(SkillTree.AGILITY, 6, DOUBLE_STRIKE, 3, 
                  "+30% critical strike chance (2x damage)"),
     TIME_DILATION(SkillTree.AGILITY, 10, PHASE_SHIFT, 5, 
                   "Ultimate: Take an extra turn");
     
     private final SkillTree tree;
     private final int levelRequired;
     private final Skill prerequisite;
     private final int maxRank;
     private final String description;
     
     public boolean canLearn(int petLevel, Map<Skill, Integer> allocatedSkills) {
       if (petLevel < levelRequired) return false;
       if (prerequisite == null) return true;
       return allocatedSkills.getOrDefault(prerequisite, 0) > 0;
     }
   }
   ```

2. **Skill Allocation Commands**
   ```java
   record AllocateSkillPointCommand(
     String petId, 
     Skill skill, 
     int points, 
     Instant timestamp
   ) {}
   
   record SkillPointAllocatedEvent(
     String petId,
     Skill skill,
     int pointsAllocated,
     int remainingPoints,
     Map<Skill, Integer> newSkillMap,
     Instant timestamp
   ) {}
   
   record RespecCommand(String petId, String respecTokenId) {}
   
   record SkillsResetEvent(
     String petId,
     Map<Skill, Integer> oldSkills,
     int refundedPoints,
     Instant timestamp
   ) {}
   ```

3. **Aggregate Updates**
   ```java
   @CommandHandler
   void handle(AllocateSkillPointCommand cmd) {
     // Validate: has skill points, meets prerequisites, not at max rank
     if (unspentSkillPoints < cmd.points()) {
       throw new InsufficientSkillPointsException("Need " + cmd.points());
     }
     
     if (!cmd.skill().canLearn(this.level, this.allocatedSkills)) {
       throw new SkillPrerequisiteNotMetException(
         "Must learn " + cmd.skill().prerequisite() + " first"
       );
     }
     
     int currentRank = allocatedSkills.getOrDefault(cmd.skill(), 0);
     if (currentRank + cmd.points() > cmd.skill().maxRank()) {
       throw new SkillMaxRankException("Skill already at max rank");
     }
     
     Map<Skill, Integer> newSkills = new HashMap<>(allocatedSkills);
     newSkills.put(cmd.skill(), currentRank + cmd.points());
     
     apply(new SkillPointAllocatedEvent(
       petId,
       cmd.skill(),
       cmd.points(),
       unspentSkillPoints - cmd.points(),
       newSkills,
       cmd.timestamp()
     ));
   }
   
   @CommandHandler
   void handle(RespecCommand cmd) {
     // Validate: has respec token in inventory (query)
     InventoryView inventory = queryGateway.query(
       new GetInventoryQuery(cmd.petId()), 
       InventoryView.class
     ).join();
     
     if (!inventory.hasItem(ItemType.RESPEC_TOKEN)) {
       throw new MissingItemException("Need respec token");
     }
     
     // Calculate total points to refund
     int totalPoints = allocatedSkills.values().stream()
       .mapToInt(Integer::intValue)
       .sum();
     
     apply(new SkillsResetEvent(
       petId,
       new HashMap<>(allocatedSkills),
       totalPoints,
       cmd.timestamp()
     ));
     
     // Consume respec token
     commandGateway.send(new UseItemCommand(
       inventory.inventoryId(), 
       ItemType.RESPEC_TOKEN, 
       1
     ));
   }
   
   @EventSourcingHandler
   void on(SkillPointAllocatedEvent event) {
     this.allocatedSkills = new HashMap<>(event.newSkillMap());
     this.unspentSkillPoints = event.remainingPoints();
   }
   
   @EventSourcingHandler
   void on(SkillsResetEvent event) {
     this.allocatedSkills.clear();
     this.unspentSkillPoints += event.refundedPoints();
   }
   ```

4. **Skill Tree Visualization (CLI)**
   ```bash
   > skills <petId>
   ═══════════════════════════════════════════════════════════
   Fluffy's Skill Trees (Level 5, 3 unspent points)
   ═══════════════════════════════════════════════════════════
   
   🔥 FIRE TREE (Offensive)
   ├─ [●●○] Flame Breath I (2/2) - Basic fire attack, +10 dmg
   ├─ [●○○] Flame Breath II (1/3) - Improved fire, +25 dmg
   ├─ [✗✗✗] Inferno (0/3) - LOCKED (Need Flame Breath II rank 2)
   └─ [✗✗✗✗✗] Dragon's Fury (0/5) - LOCKED (Level 10 required)
   
   🛡️ SCALES TREE (Defensive)
   ├─ [○○] Hardened Scales I (0/2) - AVAILABLE
   ├─ [✗✗] Hardened Scales II (0/2) - LOCKED
   ├─ [✗✗] Regeneration (0/2) - LOCKED
   ├─ [✗✗✗] Iron Hide (0/3) - LOCKED
   └─ [✗✗✗✗✗] Immortal Shell (0/5) - LOCKED (Level 10 required)
   
   ⚡ AGILITY TREE (Utility)
   ├─ [○] Quick Reflexes (0/1) - AVAILABLE
   ├─ [✗✗] Double Strike (0/2) - LOCKED
   ├─ [✗✗] Phase Shift (0/2) - LOCKED
   ├─ [✗✗✗] Critical Hit (0/3) - LOCKED
   └─ [✗✗✗✗✗] Time Dilation (0/5) - LOCKED (Level 10 required)
   
   Legend: ● = Allocated, ○ = Available, ✗ = Locked
   
   > allocate <petId> FLAME_BREATH_II 2
   Allocated 2 points to Flame Breath II!
   Flame Breath II is now rank 3 (max rank)
   Remaining skill points: 1
   
   New unlocks:
   - Inferno is now available (prerequisite met)
   ```

5. **Build Presets**
   ```java
   enum BuildPreset {
     GLASS_CANNON(
       Map.of(
         Skill.FLAME_BREATH_I, 2,
         Skill.FLAME_BREATH_II, 3,
         Skill.INFERNO, 3
       ),
       "High damage, low defense"
     ),
     TANK(
       Map.of(
         Skill.HARDENED_SCALES_I, 2,
         Skill.HARDENED_SCALES_II, 2,
         Skill.REGENERATION, 2,
         Skill.IRON_HIDE, 3
       ),
       "High survivability, low damage"
     ),
     BALANCED(
       Map.of(
         Skill.FLAME_BREATH_I, 2,
         Skill.HARDENED_SCALES_I, 2,
         Skill.QUICK_REFLEXES, 1
       ),
       "Jack of all trades"
     );
     
     private final Map<Skill, Integer> allocation;
     private final String description;
   }
   ```

6. **Projection Updates**
   ```java
   @EventHandler
   void on(SkillPointAllocatedEvent event) {
     ProgressionView view = repository.findById(event.petId()).orElseThrow();
     view.setAllocatedSkills(event.newSkillMap());
     view.setUnspentSkillPoints(event.remainingPoints());
     view.setPrimaryTree(determinePrimaryTree(event.newSkillMap()));
     repository.save(view);
   }
   
   private SkillTree determinePrimaryTree(Map<Skill, Integer> skills) {
     Map<SkillTree, Integer> treeTotals = new HashMap<>();
     
     skills.forEach((skill, points) -> {
       SkillTree tree = skill.tree();
       treeTotals.merge(tree, points, Integer::sum);
     });
     
     return treeTotals.entrySet().stream()
       .max(Map.Entry.comparingByValue())
       .map(Map.Entry::getKey)
       .orElse(null);
   }
   ```

### Technical Notes
- Skills have prerequisites forming a dependency tree
- Max rank prevents over-investment in single skill
- Respec tokens are rare inventory items (earned from dungeons)
- Primary tree is auto-detected based on most points allocated
- CLI visualization uses Unicode for clean display

---

## Phase 17: Basic Combat System

**Goal:** Implement turn-based combat in dungeons, with skill effects and enemy AI.

### Deliverables

1. **DungeonRun Aggregate**
   ```java
   @Aggregate
   class DungeonRun {
     @AggregateIdentifier
     private String runId;
     private String petId;
     private int currentFloor;
     private DungeonState state; // IN_PROGRESS, VICTORY, RETREATED, DEFEATED
     private int petCurrentHP;
     private int petMaxHP;
     private List<Enemy> enemies;
     private int turnNumber;
     private CombatPhase phase; // PLAYER_TURN, ENEMY_TURN, RESOLUTION
     
     @CommandHandler
     public DungeonRun(StartDungeonRunCommand cmd) {
       // Query pet's current stats and skills
       PetStatus status = queryGateway.query(
         new GetPetStatusQuery(cmd.petId()), 
         PetStatus.class
       ).join();
       
       ProgressionView progression = queryGateway.query(
         new GetProgressionQuery(cmd.petId()), 
         ProgressionView.class
       ).join();
       
       int maxHP = calculateMaxHP(status, progression);
       List<Enemy> floor1Enemies = generateEnemies(1, progression.level());
       
       apply(new DungeonRunStartedEvent(
         cmd.runId(),
         cmd.petId(),
         1, // Starting floor
         maxHP,
         floor1Enemies,
         cmd.timestamp()
       ));
     }
     
     @CommandHandler
     void handle(PlayerAttackCommand cmd) {
       // Validate: is player's turn, pet alive, target exists
       if (phase != CombatPhase.PLAYER_TURN) {
         throw new NotPlayerTurnException();
       }
       
       if (petCurrentHP <= 0) {
         throw new PetDefeatedException();
       }
       
       Enemy target = findEnemy(cmd.targetEnemyId());
       if (target == null || target.isDead()) {
         throw new InvalidTargetException();
       }
       
       // Query pet's skills for damage calculation
       ProgressionView progression = queryGateway.query(
         new GetProgressionQuery(petId), 
         ProgressionView.class
       ).join();
       
       int damage = calculateDamage(cmd.skillUsed(), progression);
       boolean isCritical = rollCritical(progression);
       
       if (isCritical) {
         damage *= 2;
       }
       
       apply(new PlayerAttackExecutedEvent(
         runId,
         cmd.targetEnemyId(),
         damage,
         isCritical,
         cmd.skillUsed(),
         cmd.timestamp()
       ));
       
       // Check if enemy died
       Enemy updatedEnemy = target.takeDamage(damage);
       if (updatedEnemy.isDead()) {
         apply(new EnemyDefeatedEvent(
           runId,
           cmd.targetEnemyId(),
           updatedEnemy.experienceReward(),
           cmd.timestamp()
         ));
       }
       
       // Check if all enemies dead (floor cleared)
       if (enemies.stream().allMatch(Enemy::isDead)) {
         apply(new FloorClearedEvent(runId, currentFloor, cmd.timestamp()));
       } else {
         // Move to enemy turn
         apply(new TurnPhaseChangedEvent(
           runId, 
           CombatPhase.ENEMY_TURN, 
           cmd.timestamp()
         ));
       }
     }
     
     @CommandHandler
     void handle(EnemyTurnCommand cmd) {
       // AI selects action for each alive enemy
       for (Enemy enemy : enemies) {
         if (!enemy.isDead()) {
           EnemyAction action = enemy.selectAction(this);
           int damage = enemy.calculateDamage();
           
           apply(new EnemyAttackExecutedEvent(
             runId,
             enemy.id(),
             damage,
             action,
             cmd.timestamp()
           ));
         }
       }
       
       // Check if pet died
       if (petCurrentHP <= 0) {
         apply(new PetDefeatedInDungeonEvent(runId, petId, currentFloor, cmd.timestamp()));
       } else {
         // Move back to player turn
         apply(new TurnPhaseChangedEvent(
           runId,
           CombatPhase.PLAYER_TURN,
           cmd.timestamp()
         ));
       }
     }
     
     @CommandHandler
     void handle(RetreatFromDungeonCommand cmd) {
       // Can only retreat during player turn
       if (phase != CombatPhase.PLAYER_TURN) {
         throw new CannotRetreatException("Not your turn");
       }
       
       apply(new DungeonRunEndedEvent(
         runId,
         petId,
         currentFloor,
         DungeonState.RETREATED,
         calculateXPReward(currentFloor, DungeonState.RETREATED),
         cmd.timestamp()
       ));
     }
     
     @EventSourcingHandler
     void on(DungeonRunStartedEvent event) {
       this.runId = event.runId();
       this.petId = event.petId();
       this.currentFloor = event.floor();
       this.petMaxHP = event.maxHP();
       this.petCurrentHP = event.maxHP();
       this.enemies = new ArrayList<>(event.enemies());
       this.state = DungeonState.IN_PROGRESS;
       this.phase = CombatPhase.PLAYER_TURN;
       this.turnNumber = 1;
     }
     
     @EventSourcingHandler
     void on(PlayerAttackExecutedEvent event) {
       Enemy target = findEnemy(event.targetEnemyId());
       target.takeDamage(event.damage());
     }
     
     @EventSourcingHandler
     void on(EnemyAttackExecutedEvent event) {
       this.petCurrentHP = Math.max(0, petCurrentHP - event.damage());
     }
     
     @EventSourcingHandler
     void on(FloorClearedEvent event) {
       this.currentFloor++;
       this.enemies = generateEnemies(currentFloor, getPetLevel());
       this.phase = CombatPhase.PLAYER_TURN;
       this.turnNumber = 1;
     }
     
     @EventSourcingHandler
     void on(DungeonRunEndedEvent event) {
       this.state = event.finalState();
     }
     
     private int calculateDamage(Skill skill, ProgressionView progression) {
       int baseDamage = petMaxHP / 10; // Base damage scales with HP
       int skillRank = progression.allocatedSkills().getOrDefault(skill, 0);
       
       return switch(skill) {
         case FLAME_BREATH_I -> baseDamage + (10 * skillRank);
         case FLAME_BREATH_II -> baseDamage + (25 * skillRank);
         case INFERNO -> (baseDamage + (40 * skillRank)) * enemies.size(); // AoE
         case null -> baseDamage; // Basic attack
         default -> baseDamage;
       };
     }
     
     private boolean rollCritical(ProgressionView progression) {
       int criticalRank = progression.allocatedSkills()
         .getOrDefault(Skill.CRITICAL_HIT, 0);
       double critChance = 0.05 + (criticalRank * 0.10); // 5% base + 10% per rank
       return Math.random() < critChance;
     }
     
     private int calculateMaxHP(PetStatus status, ProgressionView progression) {
       int baseHP = status.health() * 2; // 100 health = 200 HP
       int scalesRank = progression.allocatedSkills()
         .getOrDefault(Skill.HARDENED_SCALES_I, 0);
       return baseHP + (scalesRank * 20); // +20 HP per rank
     }
   }
   ```

2. **Enemy System**
   ```java
   record Enemy(
     String id,
     EnemyType type,
     int currentHP,
     int maxHP,
     int attack,
     int defense,
     EnemyAI ai,
     int experienceReward
   ) {
     
     boolean isDead() {
       return currentHP <= 0;
     }
     
     Enemy takeDamage(int damage) {
       int actualDamage = Math.max(1, damage - defense);
       int newHP = Math.max(0, currentHP - actualDamage);
       return new Enemy(id, type, newHP, maxHP, attack, defense, ai, experienceReward);
     }
     
     EnemyAction selectAction(DungeonRun run) {
       return ai.chooseAction(this, run);
     }
     
     int calculateDamage() {
       return attack + (int)(Math.random() * 5); // Attack ± random variance
     }
   }
   
   enum EnemyType {
     GOBLIN(30, 10, 5, 10, EnemyAI.AGGRESSIVE),
     WOLF(50, 15, 3, 15, EnemyAI.PACK_HUNTER),
     SKELETON(40, 12, 8, 20, EnemyAI.DEFENSIVE),
     ORC(80, 20, 10, 30, EnemyAI.AGGRESSIVE),
     DRAGON_BOSS(200, 30, 15, 100, EnemyAI.TACTICAL);
     
     private final int hp;
     private final int attack;
     private final int defense;
     private final int xpReward;
     private final EnemyAI ai;
   }
   
   enum EnemyAI {
     AGGRESSIVE {
       EnemyAction chooseAction(Enemy self, DungeonRun run) {
         // Always attack, no strategy
         return EnemyAction.ATTACK;
       }
     },
     DEFENSIVE {
       EnemyAction chooseAction(Enemy self, DungeonRun run) {
         // Defend if health low
         if (self.currentHP() < self.maxHP() * 0.3) {
           return EnemyAction.DEFEND;
         }
         return EnemyAction.ATTACK;
       }
     },
     PACK_HUNTER {
       EnemyAction chooseAction(Enemy self, DungeonRun run) {
         // Attack if other wolves alive, defend if alone
         long aliveAllies = run.enemies().stream()
           .filter(e -> e.type() == self.type() && !e.isDead())
           .count();
         return aliveAllies > 1 ? EnemyAction.ATTACK : EnemyAction.DEFEND;
       }
     },
     TACTICAL {
       EnemyAction chooseAction(Enemy self, DungeonRun run) {
         // Complex logic: strong attack every 3 turns, otherwise attack/defend based on HP
         if (run.turnNumber() % 3 == 0) {
           return EnemyAction.POWER_ATTACK;
         }
         return self.currentHP() < self.maxHP() * 0.5 ? 
           EnemyAction.DEFEND : EnemyAction.ATTACK;
       }
     }
   }
   
   enum EnemyAction {
     ATTACK,        // Normal damage
     DEFEND,        // Reduce incoming damage by 50% this turn
     POWER_ATTACK,  // 1.5x damage
     HEAL           // Restore 20% HP
   }
   ```

3. **Enemy Generation**
   ```java
   List<Enemy> generateEnemies(int floor, int petLevel) {
     int difficulty = floor + (petLevel / 2);
     
     return switch(floor) {
       case 1 -> List.of(
         createEnemy(EnemyType.GOBLIN, difficulty)
       );
       case 2 -> List.of(
         createEnemy(EnemyType.GOBLIN, difficulty),
         createEnemy(EnemyType.GOBLIN, difficulty)
       );
       case 3 -> List.of(
         createEnemy(EnemyType.WOLF, difficulty)
       );
       case 4, 5 -> List.of(
         createEnemy(EnemyType.WOLF, difficulty),
         createEnemy(EnemyType.GOBLIN, difficulty)
       );
       case 6, 7, 8 -> List.of(
         createEnemy(EnemyType.SKELETON, difficulty),
         createEnemy(EnemyType.SKELETON, difficulty)
       );
       case 9 -> List.of(
         createEnemy(EnemyType.ORC, difficulty)
       );
       case 10 -> List.of(
         createEnemy(EnemyType.DRAGON_BOSS, difficulty) // Boss fight!
       );
       default -> List.of(
         createEnemy(EnemyType.ORC, difficulty),
         createEnemy(EnemyType.SKELETON, difficulty)
       );
     };
   }
   
   Enemy createEnemy(EnemyType type, int difficulty) {
     double multiplier = 1.0 + (difficulty * 0.1); // +10% per difficulty
     
     return new Enemy(
       UUID.randomUUID().toString(),
       type,
       (int)(type.hp() * multiplier),
       (int)(type.hp() * multiplier),
       (int)(type.attack() * multiplier),
       (int)(type.defense() * multiplier),
       type.ai(),
       (int)(type.xpReward() * multiplier)
     );
   }
   ```

4. **Combat Resolution Saga**
   ```java
   @Saga
   class CombatResolutionSaga {
     
     @SagaEventHandler(associationProperty = "runId")
     void on(TurnPhaseChangedEvent event) {
       if (event.newPhase() == CombatPhase.ENEMY_TURN) {
         // Trigger enemy AI to act
         commandGateway.send(new EnemyTurnCommand(event.runId(), event.timestamp()));
       }
     }
     
     @SagaEventHandler(associationProperty = "runId")
     void on(FloorClearedEvent event) {
       // Award XP for clearing floor
       commandGateway.send(new AwardXPCommand(
         event.petId(),
         event.floor() * 50, // 50 XP per floor
         "FLOOR_CLEARED",
         event.timestamp()
       ));
       
       // Check if player wants to continue or retreat (wait for command)
     }
     
     @EndingSagaEventHandler
     void on(DungeonRunEndedEvent event) {
       // Award final XP reward
       commandGateway.send(new AwardXPCommand(
         event.petId(),
         event.xpReward(),
         "DUNGEON_COMPLETED",
         event.timestamp()
       ));
       
       // If pet died, trigger death event in Pet aggregate
       if (event.finalState() == DungeonState.DEFEATED) {
         commandGateway.send(new KillPetCommand(
           event.petId(),
           "Died in dungeon on floor " + event.finalFloor()
         ));
       }
     }
   }
   ```

5. **CLI Combat Interface**
   ```bash
   > dungeon start <petId>
   ═══════════════════════════════════════════════════════════
   Entering Dungeon with Fluffy (Level 5, Fire Build)
   ═══════════════════════════════════════════════════════════
   
   Floor 1 - The Goblin Cave
   
   Fluffy: 200/200 HP
   Enemies:
   1. Goblin (30/30 HP) - Attack: 10, Defense: 5
   
   Your turn!
   1. Attack with Basic Attack
   2. Attack with Flame Breath I
   3. Attack with Flame Breath II
   4. Retreat
   
   > 2
   
   Fluffy uses Flame Breath I on Goblin!
   → 35 damage dealt (25 base + 10 skill bonus)
   
   Goblin: 0/30 HP - DEFEATED! (+10 XP)
   
   ═══════════════════════════════════════════════════════════
   Floor 1 Cleared! Continue to Floor 2?
   ═══════════════════════════════════════════════════════════
   1. Continue
   2. Retreat (keep all XP earned)
   
   > 1
   
   Floor 2 - The Dark Passage
   
   Fluffy: 200/200 HP
   Enemies:
   1. Goblin (30/30 HP)
   2. Goblin (30/30 HP)
   
   Your turn!
   1. Attack Goblin #1 with Basic Attack
   2. Attack Goblin #1 with Flame Breath I
   3. Attack Goblin #2 with Flame Breath I
   4. Use Inferno (AoE, hits both enemies)
   5. Retreat
   
   > 4
   
   Fluffy uses Inferno!
   → Goblin #1 takes 50 damage (DEFEATED!)
   → Goblin #2 takes 50 damage (DEFEATED!)
   
   ═══════════════════════════════════════════════════════════
   Floor 2 Cleared! (+50 XP)
   Fluffy: 200/200 HP
   Total XP earned this run: 110
   ═══════════════════════════════════════════════════════════
   ```

6. **Projection for Combat History**
   ```java
   @Component
   @ProcessingGroup("dungeon-history")
   class DungeonHistoryProjection {
     
     @EventHandler
     void on(DungeonRunEndedEvent event) {
       DungeonRunSummary summary = new DungeonRunSummary(
         event.runId(),
         event.petId(),
         event.finalFloor(),
         event.finalState(),
         event.xpReward(),
         event.timestamp()
       );
       repository.save(summary);
     }
     
     @QueryHandler
     List<DungeonRunSummary> handle(GetDungeonHistoryQuery query) {
       return repository.findByPetId(query.petId())
         .stream()
         .sorted(Comparator.comparing(DungeonRunSummary::timestamp).reversed())
         .limit(10)
         .toList();
     }
     
     @QueryHandler
     DungeonStatistics handle(GetDungeonStatsQuery query) {
       List<DungeonRunSummary> history = repository.findByPetId(query.petId());
       
       return new DungeonStatistics(
         history.size(),
         history.stream().mapToInt(DungeonRunSummary::finalFloor).max().orElse(0),
         history.stream().mapToInt(DungeonRunSummary::finalFloor).average().orElse(0),
         history.stream().filter(r -> r.state() == DungeonState.RETREATED).count(),
         history.stream().filter(r -> r.state() == DungeonState.DEFEATED).count()
       );
     }
   }
   ```

### Technical Notes
- Combat is turn-based with discrete phases (player → enemy → resolution)
- Damage calculations query progression projection for skill ranks
- Enemy AI is deterministic given same game state (important for event sourcing)
- Retreat allows players to keep earned XP but forfeit floor progression
- Death in dungeon triggers Pet aggregate death (permanent unless resurrection mechanic)

---

## Phase 18: Dungeon Progression & Boss Fights

**Goal:** Add multi-floor dungeons with increasing difficulty, boss mechanics, and loot drops.

### Deliverables

1. **Boss Mechanics**
   ```java
   interface BossAbility {
     void execute(DungeonRun run);
   }
   
   class DragonBoss extends Enemy {
     private final List<BossAbility> abilities;
     private int phase; // 1, 2, 3 (gets more aggressive as HP drops)
     
     @Override
     EnemyAction selectAction(DungeonRun run) {
       updatePhase();
       
       return switch(phase) {
         case 1 -> // Conservative, normal attacks
           EnemyAction.ATTACK;
         case 2 -> // Aggressive, power attacks
           run.turnNumber() % 2 == 0 ? EnemyAction.POWER_ATTACK : EnemyAction.ATTACK;
         case 3 -> // Desperate, uses special abilities
           executeSpecialAbility(run);
       };
     }
     
     private void updatePhase() {
       double hpPercent = (double)currentHP() / maxHP();
       if (hpPercent > 0.66) phase = 1;
       else if (hpPercent > 0.33) phase = 2;
       else phase = 3;
     }
     
     private EnemyAction executeSpecialAbility(DungeonRun run) {
       // Phase 3: Dragon uses breath attack (AoE) or wing buffet (knockback)
       return Math.random() < 0.5 ? 
         EnemyAction.BREATH_ATTACK : 
         EnemyAction.WING_BUFFET;
     }
   }
   ```

2. **Loot System**
   ```java
   enum LootRarity {
     COMMON(0.60),    // 60% drop chance
     UNCOMMON(0.30),  // 30% drop chance
     RARE(0.09),      // 9% drop chance
     LEGENDARY(0.01); // 1% drop chance
     
     private final double dropChance;
   }
   
   record LootDrop(
     ItemType item,
     int quantity,
     LootRarity rarity
   ) {}
   
   class LootTable {
     static List<LootDrop> generateLoot(int floor, EnemyType enemy) {
       List<LootDrop> drops = new ArrayList<>();
       
       // Boss enemies always drop loot
       if (enemy == EnemyType.DRAGON_BOSS) {
         drops.add(new LootDrop(ItemType.RESPEC_TOKEN, 1, LootRarity.RARE));
         drops.add(new LootDrop(ItemType.SKILL_BOOK, 1, LootRarity.LEGENDARY));
       }
       
       // Regular enemies have chance to drop
       double roll = Math.random();
       if (roll < 0.3) { // 30% drop chance
         LootRarity rarity = rollRarity();
         ItemType item = selectItemByRarity(rarity, floor);
         drops.add(new LootDrop(item, 1, rarity));
       }
       
       return drops;
     }
     
     private static LootRarity rollRarity() {
       double roll = Math.random();
       if (roll < LootRarity.LEGENDARY.dropChance) return LootRarity.LEGENDARY;
       if (roll < LootRarity.RARE.dropChance + LootRarity.LEGENDARY.dropChance) 
         return LootRarity.RARE;
       if (roll < LootRarity.UNCOMMON.dropChance + LootRarity.RARE.dropChance + 
                  LootRarity.LEGENDARY.dropChance) 
         return LootRarity.UNCOMMON;
       return LootRarity.COMMON;
     }
   }
   ```

3. **New Item Types**
   ```java
   enum ItemType {
     // Existing
     APPLE, PIZZA, MEDICINE, BALL, ROBOT,
     
     // New loot drops
     HEALTH_POTION(LootRarity.COMMON, "Restore 50 HP in dungeon"),
     MANA_POTION(LootRarity.COMMON, "Restore skill cooldowns"),
     RESPEC_TOKEN(LootRarity.RARE, "Reset all skill points"),
     SKILL_BOOK(LootRarity.LEGENDARY, "Learn a random skill instantly"),
     ENCHANTED_COLLAR(LootRarity.RARE, "+10% all damage"),
     IRON_ARMOR(LootRarity.UNCOMMON, "+20 defense in dungeons"),
     SPEED_BOOTS(LootRarity.UNCOMMON, "+15% dodge chance");
   }
   ```

4. **Equipment System**
   ```java
   record Equipment(
     String petId,
     ItemType collar,
     ItemType armor,
     ItemType accessory
   ) {
     
     int calculateBonusDamage() {
       int bonus = 0;
       if (collar == ItemType.ENCHANTED_COLLAR) bonus += 10; // +10% damage
       return bonus;
     }
     
     int calculateBonusDefense() {
       int bonus = 0;
       if (armor == ItemType.IRON_ARMOR) bonus += 20;
       return bonus;
     }
     
     double calculateDodgeBonus() {
       double bonus = 0.0;
       if (accessory == ItemType.SPEED_BOOTS) bonus += 0.15;
       return bonus;
     }
   }
   
   @CommandHandler
   void handle(EquipItemCommand cmd) {
     // Validate: item exists in inventory, is equipment type
     apply(new ItemEquippedEvent(
       cmd.petId(),
       cmd.item(),
       cmd.slot(),
       cmd.timestamp()
     ));
   }
   ```

5. **Loot Award Saga**
   ```java
   @Saga
   class LootAwardSaga {
     
     @SagaEventHandler(associationProperty = "runId")
     void on(EnemyDefeatedEvent event) {
       // Roll for loot
       DungeonRun run = queryGateway.query(
         new GetDungeonRunQuery(event.runId()), 
         DungeonRun.class
       ).join();
       
       Enemy enemy = run.findEnemy(event.enemyId());
       List<LootDrop> loot = LootTable.generateLoot(run.currentFloor(), enemy.type());
       
       if (!loot.isEmpty()) {
         // Add items to inventory
         for (LootDrop drop : loot) {
           commandGateway.send(new AddItemCommand(
             run.petId(),
             drop.item(),
             drop.quantity()
           ));
         }
         
         apply(new LootAwardedEvent(
           event.runId(),
           event.enemyId(),
           loot,
           event.timestamp()
         ));
       }
     }
   }
   ```

6. **Dungeon Difficulty Curve**
   ```java
   class DungeonBalancer {
     static int calculateDifficulty(int floor, int petLevel) {
       // Difficulty increases with floors, but scales with pet level
       // Floor 1 at level 5 should be easier than floor 1 at level 1
       
       int baseDifficulty = floor * 10;
       int levelAdjustment = Math.max(0, petLevel - floor);
       
       return baseDifficulty - levelAdjustment;
     }
     
     static int calculateXPReward(int floor, DungeonState outcome) {
       int baseReward = floor * 50;
       
       return switch(outcome) {
         case VICTORY -> baseReward * 2; // Double XP for full clear
         case RETREATED -> baseReward; // Normal XP
         case DEFEATED -> baseReward / 2; // Half XP for death
         default -> 0;
       };
     }
   }
   ```

7. **CLI Enhancements**
   ```bash
   > dungeon stats <petId>
   ═══════════════════════════════════════════════════════════
   Fluffy's Dungeon Statistics
   ═══════════════════════════════════════════════════════════
   Total Runs: 15
   Deepest Floor: 8
   Average Floor: 4.2
   Retreats: 8
   Deaths: 2
   Victory Rate: 33%
   
   Recent Runs:
   1. Floor 8 (Retreated) - 400 XP - 2 hours ago
   2. Floor 6 (Defeated) - 150 XP - 5 hours ago
   3. Floor 5 (Retreated) - 250 XP - 1 day ago
   
   > inventory equipment
   ═══════════════════════════════════════════════════════════
   Equipped Items
   ═══════════════════════════════════════════════════════════
   Collar: Enchanted Collar (+10% damage)
   Armor: Iron Armor (+20 defense)
   Accessory: Speed Boots (+15% dodge)
   
   Bonuses:
   - Total Damage Bonus: +10%
   - Total Defense Bonus: +20
   - Total Dodge Bonus: +15%
   ```

### Technical Notes
- Boss fights have phases that change behavior based on HP
- Loot drops are probabilistic but deterministic (seed-based RNG)
- Equipment provides passive bonuses during combat
- Difficulty scales with both floor number and pet level (dynamic balancing)
- XP rewards incentivize risk-taking (deeper floors = more XP)

---

## Phase 19: Polish, Balance & Leaderboards

**Goal:** Add final quality-of-life features, tune game balance, and implement competitive leaderboards.

### Deliverables

1. **Combat Logs**
   ```java
   @Component
   class CombatLogger {
     private final Map<String, List<CombatLogEntry>> logs = new ConcurrentHashMap<>();
     
     @EventHandler
     void on(PlayerAttackExecutedEvent event) {
       addLog(event.runId(), new CombatLogEntry(
         event.timestamp(),
         "Player",
         "Fluffy used " + event.skillUsed() + " for " + event.damage() + " damage" +
         (event.isCritical() ? " (CRITICAL!)" : "")
       ));
     }
     
     @EventHandler
     void on(EnemyAttackExecutedEvent event) {
       addLog(event.runId(), new CombatLogEntry(
         event.timestamp(),
         "Enemy",
         event.enemyId() + " attacked for " + event.damage() + " damage"
       ));
     }
     
     @QueryHandler
     List<CombatLogEntry> handle(GetCombatLogQuery query) {
       return logs.getOrDefault(query.runId(), List.of());
     }
   }
   ```

2. **Replay System**
   ```bash
   > dungeon replay <runId>
   ═══════════════════════════════════════════════════════════
   Replaying Dungeon Run: a3f2c9e1
   ═══════════════════════════════════════════════════════════
   
   Floor 1 - The Goblin Cave
   Turn 1: Fluffy used Flame Breath I → Goblin (35 damage)
          Goblin DEFEATED!
   
   Floor 2 - The Dark Passage
   Turn 1: Fluffy used Inferno → Goblin #1 (50 damage), Goblin #2 (50 damage)
          Both enemies DEFEATED!
   
   Floor 3 - The Wolf Den
   Turn 1: Fluffy used Flame Breath II → Wolf (45 damage)
   Turn 2: Wolf attacked → Fluffy (18 damage)
   Turn 3: Fluffy used Flame Breath II → Wolf (45 damage, CRITICAL!)
          Wolf DEFEATED!
   
   Run ended: Retreated on Floor 3
   Total XP Earned: 210
   ═══════════════════════════════════════════════════════════
   ```

3. **Global Leaderboards**
   ```java
   @Component
   @ProcessingGroup("leaderboard")
   class LeaderboardProjection {
     
     @EventHandler
     void on(DungeonRunEndedEvent event) {
       // Update deepest floor leaderboard
       LeaderboardEntry entry = repository.findByPetId(event.petId())
         .orElse(new LeaderboardEntry(event.petId()));
       
       if (event.finalFloor() > entry.deepestFloor()) {
         entry.setDeepestFloor(event.finalFloor());
         entry.setLastUpdated(event.timestamp());
         repository.save(entry);
       }
     }
     
     @EventHandler
     void on(PetLeveledUpEvent event) {
       // Update level leaderboard
       LeaderboardEntry entry = repository.findByPetId(event.petId())
         .orElse(new LeaderboardEntry(event.petId()));
       
       entry.setLevel(event.newLevel());
       entry.setLastUpdated(event.timestamp());
       repository.save(entry);
     }
     
     @QueryHandler
     List<LeaderboardEntry> handle(GetLeaderboardQuery query) {
       return switch(query.category()) {
         case DEEPEST_FLOOR -> repository.findAll()
           .stream()
           .sorted(Comparator.comparing(LeaderboardEntry::deepestFloor).reversed())
           .limit(query.limit())
           .toList();
         case HIGHEST_LEVEL -> repository.findAll()
           .stream()
           .sorted(Comparator.comparing(LeaderboardEntry::level).reversed())
           .limit(query.limit())
           .toList();
         case TOTAL_XP -> repository.findAll()
           .stream()
           .sorted(Comparator.comparing(LeaderboardEntry::totalXP).reversed())
           .limit(query.limit())
           .toList();
       };
     }
   }
   ```

4. **Balance Tuning Configuration**
   ```yaml
   game-balance:
     xp:
       feed-base: 10
       play-base: 15
       clean-base: 12
       medicine-base: 20
       perfect-day-bonus: 50
       emotional-game-success: 30
       dungeon-floor-multiplier: 50
     
     skills:
       flame-breath-i-damage: 10
       flame-breath-ii-damage: 25
       inferno-damage: 40
       hardened-scales-defense: 15
       regeneration-heal: 5
       critical-hit-chance-per-rank: 0.10
     
     combat:
       base-hp-multiplier: 2.0
       base-damage-divisor: 10
       critical-multiplier: 2.0
       defend-damage-reduction: 0.5
     
     dungeon:
       difficulty-per-floor: 10
       level-adjustment-factor: 1
       xp-victory-multiplier: 2.0
       xp-retreat-multiplier: 1.0
       xp-defeat-multiplier: 0.5
       boss-floor-interval: 10
   ```

5. **Skill Balance Analysis**
   ```java
   @Service
   class SkillAnalyzer {
     public SkillUsageReport analyzeSkillUsage() {
       // Query all progression projections
       List<ProgressionView> allPets = repository.findAll();
       
       Map<Skill, Long> skillPopularity = allPets.stream()
         .flatMap(p -> p.allocatedSkills().keySet().stream())
         .collect(Collectors.groupingBy(
           Function.identity(), 
           Collectors.counting()
         ));
       
       Map<Skill, Double> skillWinRate = calculateWinRateBySkill();
       
       return new SkillUsageReport(skillPopularity, skillWinRate);
     }
     
     private Map<Skill, Double> calculateWinRateBySkill() {
       // Correlate skills with dungeon success rates
       // Used to identify overpowered/underpowered skills
       // ...implementation
     }
   }
   ```

6. **Achievement Integration**
   ```java
   // New achievements for combat system
   FIRST_DUNGEON_RUN("Enter your first dungeon"),
   FLOOR_FIVE("Reach floor 5 in any dungeon run"),
   FLOOR_TEN("Reach floor 10 and defeat the boss"),
   PERFECT_RUN("Clear 5 floors without taking damage"),
   GLASS_CANNON("Reach floor 8 with only offensive skills"),
   TANK_MASTER("Reach floor 8 with only defensive skills"),
   SKILL_MASTER("Unlock all skills in one tree"),
   LEGENDARY_LOOT("Obtain a legendary item from dungeon"),
   SPEED_RUNNER("Clear floor 10 in under 50 turns");
   ```

7. **CLI Quality of Life**
   ```bash
   > quick-dungeon <petId>
   # Automatically starts dungeon with saved preferences
   # Auto-continues floors until death/retreat threshold
   # Useful for grinding
   
   > build export <petId>
   Exported build to: fire-dragon-build-v1.json
   Share this code with friends: FD-F2I3-S1R2-A0
   
   > build import FD-F2I3-S1R2-A0
   Imported build: "Fire Dragon Build v1"
   - Flame Breath I: 2 points
   - Flame Breath II: 3 points
   - Inferno: 3 points
   - Hardened Scales I: 1 point
   
   Apply this build to Fluffy? (costs 1 Respec Token)
   
   > battle <petId1> <petId2>
   # PvP: Two pets fight (uses dungeon combat system)
   # Asynchronous: challenge another player, they respond when ready
   ```

### Technical Notes
- Combat logs stored temporarily (cleared after 24 hours)
- Replay uses event sourcing to reconstruct exact combat sequence
- Leaderboards update in near-real-time via projection
- Balance configuration externalized for easy tuning without code changes
- Build import/export uses compressed encoding of skill allocations
- PvP reuses dungeon combat aggregate with different enemy type (player pet)

---

## Data Model Summary

### New Aggregates
- **PetProgression:** level, XP, skills, skill points
- **DungeonRun:** floor, enemies, pet HP, combat state
- **Equipment:** collar, armor, accessory slots

### Commands (Examples)
- AwardXPCommand, AllocateSkillPointCommand, RespecCommand
- StartDungeonRunCommand, PlayerAttackCommand, EnemyTurnCommand, RetreatFromDungeonCommand
- EquipItemCommand, UseHealthPotionCommand

### Events (Examples)
- ProgressionCreatedEvent, XPAwardedEvent, PetLeveledUpEvent, SkillPointAllocatedEvent
- DungeonRunStartedEvent, PlayerAttackExecutedEvent, EnemyDefeatedEvent, FloorClearedEvent, DungeonRunEndedEvent
- LootAwardedEvent, ItemEquippedEvent

### Projections
- ProgressionProjection (level, skills, XP)
- DungeonHistoryProjection (past runs)
- LeaderboardProjection (rankings)
- CombatLogProjection (turn-by-turn logs)

---

## Configuration Files

### application.yml (additions)
```yaml
game:
  progression:
    xp-curve-base: 100
    xp-curve-exponent: 1.2
    skill-points-per-level: 3
    max-level: 50
  
  combat:
    max-dungeon-floors: 20
    turn-timeout: 60s # 60 seconds to make a move
    auto-save-interval: 5 # Save after every 5 turns
  
  balance:
    config-file: classpath:game-balance.yml
    reload-interval: 5m # Reload balance config every 5 minutes

axon:
  eventhandling:
    processors:
      pet-progression:
        mode: tracking
      dungeon-history:
        mode: tracking
      leaderboard:
        mode: tracking
        batch-size: 100
```

---

## Additional Synergies with Other Ideas

### Multi-Player Integration (From Addendum #1)
**PvP Arena:**
- Use dungeon combat system with player pets as "enemies"
- Leaderboards for arena rankings
- Betting system (wager items on matches)
- Seasonal tournaments with exclusive rewards

**Guild System:**
- Guilds pool resources for guild-wide dungeon runs
- Cooperative boss raids (4 pets vs mega-boss)
- Guild leaderboards and prestige

**Build Sharing:**
- Export/import skill builds as codes
- Community-voted "meta" builds
- Theorycrafting forums with build discussions

### Exploration & World (From Addendum #1)
**Dungeon as Locations:**
- Dungeons are specific locations on the world map
- Different biomes have different enemy types
- Rare dungeons only accessible from specific exploration paths

**Location-Based Loot:**
- Ocean dungeons drop water-themed equipment
- Mountain dungeons drop cold-resistance gear
- Desert dungeons have fire-resistant enemies

**Discovery Progression:**
- Unlock new dungeon types by exploring world
- Hidden dungeons with unique boss fights

### Genetics Integration (From Addendum #1)
**Genetic Skill Affinity:**
- Pets inherit predisposition to certain skill trees
- Dragon genetics → +20% XP for Fire skills
- Cat genetics → +20% XP for Agility skills

**Trait-Based Bonuses:**
- Recessive "Berserker" trait → +damage but -defense
- Dominant "Resilient" trait → +HP regeneration
- Breed for optimal combat traits

### Base Building (From Addendum #1)
**Training Facilities:**
- Build "Dojo" in habitat → +10% XP gain
- Build "Library" → unlock skills faster
- Build "Armory" → equipment bonuses

**Passive XP Generation:**
- "Training Dummy" building generates passive XP while offline
- "Meditation Garden" slowly refills HP between dungeon runs

---

## Future Directions

### Short-Term Enhancements
1. **Skill Cooldowns:** Powerful skills have cooldowns (use once per 3 turns)
2. **Status Effects:** Burn (DoT), Freeze (skip turn), Poison (gradual damage)
3. **Combo System:** Chain skills for bonus damage
4. **Dungeon Modifiers:** Random buffs/debuffs per floor (enemies have +20% HP)
5. **Daily Dungeons:** Special timed dungeons with unique rewards

### Medium-Term Features
1. **Class System:** Pets choose a class at level 10 (Warrior, Mage, Rogue)
2. **Prestige System:** Reset to level 1 with permanent bonuses
3. **Seasonal Ladders:** Leaderboards reset quarterly with exclusive rewards
4. **Crafting:** Combine loot drops to create better equipment
5. **Pet Parties:** Bring 2-3 pets into dungeons together

### Long-Term Vision
1. **Raid System:** 10-player cooperative mega-dungeons
2. **PvP Ranked Mode:** Competitive ladder with ELO rankings
3. **Narrative Campaigns:** Story-driven dungeon sequences
4. **Procedural Dungeon Generation:** Infinite replay value
5. **Esports Integration:** Tournaments, streaming, spectator mode

---

## Key Learning Outcomes (RPG Progression)

By completing these phases, you will gain hands-on experience with:

✅ **Complex Business Logic:** XP curves, skill trees, combat resolution formulas  
✅ **Multi-Aggregate Coordination:** Progression + Combat + Inventory working together  
✅ **Saga Orchestration:** Combat flow, loot distribution, XP awards  
✅ **Performance Optimization:** Combat logs, leaderboards, bulk queries  
✅ **Game Balance:** Tuning numbers, analyzing metrics, preventing exploits  
✅ **Event Replay:** Combat logs, dungeon replay from event stream  
✅ **Snapshot Optimization:** Long dungeon runs benefit from snapshots  
✅ **Query Performance:** Leaderboards require indexed projections  
✅ **Deterministic Randomness:** Combat RNG must be reproducible  
✅ **Real-Time Processing:** Turn-based coordination with timeouts

---

## Implementation Priority

**Must-Have (MVP):**
- Phase 15: XP & Leveling
- Phase 16: Skill Trees
- Phase 17: Basic Combat

**Should-Have (Polish):**
- Phase 18: Dungeon Progression
- Phase 19: Leaderboards

**Nice-to-Have (Post-Launch):**
- PvP Arena
- Guild System
- Seasonal Content

---

## Testing Strategy

### Unit Tests
```java
@Test
void testXPAwardAndLevelUp() {
  fixture.given(
    new ProgressionCreatedEvent("pet1", 1, 0, 0)
  )
  .when(new AwardXPCommand("pet1", 100, "FEED"))
  .expectEvents(
    new XPAwardedEvent("pet1", 100, 100, "FEED"),
    new PetLeveledUpEvent("pet1", 1, 2, 3)
  );
}

@Test
void testSkillAllocation() {
  fixture.given(
    new ProgressionCreatedEvent("pet1", 1, 0, 0),
    new PetLeveledUpEvent("pet1", 1, 2, 3)
  )
  .when(new AllocateSkillPointCommand("pet1", Skill.FLAME_BREATH_I, 2))
  .expectEvents(
    new SkillPointAllocatedEvent("pet1", Skill.FLAME_BREATH_I, 2, 1, ...)
  );
}

@Test
void testCombatDamageCalculation() {
  DungeonRun run = createTestDungeon();
  ProgressionView progression = createFireBuildProgression();
  
  int damage = run.calculateDamage(Skill.FLAME_BREATH_II, progression);
  
  assertEquals(45, damage); // 20 base + 25 skill
}
```

### Integration Tests
```java
@Test
void testFullDungeonRun() {
  // Start dungeon
  commandGateway.send(new StartDungeonRunCommand("run1", "pet1")).join();
  
  // Attack enemy
  commandGateway.send(new PlayerAttackCommand("run1", "enemy1", Skill.FLAME_BREATH_I)).join();
  
  // Query combat state
  DungeonRun run = queryGateway.query(new GetDungeonRunQuery("run1")).join();
  
  assertTrue(run.enemies().get(0).isDead());
  assertEquals(2, run.currentFloor());
}
```

### Balance Testing
```java
@Test
void testSkillBalanceDamagePerPoint() {
  // Ensure no skill is strictly better than others
  ProgressionView fire = buildWithSkill(Skill.FLAME_BREATH_II, 3);
  ProgressionView scales = buildWithSkill(Skill.HARDENED_SCALES_II, 3);
  
  int fireDamage = calculateDPT(fire); // Damage per turn
  int scalesSurvival = calculateTTK(scales); // Turns to kill
  
  // Balanced: fire should have higher DPS, scales should survive longer
  assertTrue(fireDamage > scalesSurvival * 1.2);
}
```

### Load Testing
```java
@Test
void testLeaderboardPerformance() {
  // Create 10,000 pets with varying levels
  IntStream.range(0, 10000).forEach(i -> {
    createPet("pet" + i, randomLevel());
  });
  
  StopWatch stopWatch = new StopWatch();
  stopWatch.start();
  
  List<LeaderboardEntry> top100 = queryGateway.query(
    new GetLeaderboardQuery(LeaderboardCategory.HIGHEST_LEVEL, 100)
  ).join();
  
  stopWatch.stop();
  
  assertTrue(stopWatch.getTime() < 100); // Should return in under 100ms
  assertEquals(100, top100.size());
}
```

---

## Troubleshooting Guide

### Common Issues

**Issue:** XP not being awarded after care actions  
**Solution:** Check Care Reward Saga associations, verify event handlers are subscribed

**Issue:** Skills not affecting combat damage  
**Solution:** Verify combat saga queries progression projection correctly

**Issue:** Dungeon run events replaying slowly  
**Solution:** Implement snapshots for DungeonRun aggregate

**Issue:** Leaderboard not updating  
**Solution:** Check projection processor is tracking, not subscribing

**Issue:** Combat feels unbalanced (one skill dominates)  
**Solution:** Adjust balance config, run skill usage analysis

**Issue:** Players exploiting XP farming  
**Solution:** Add rate limiting, diminishing returns for repetitive actions

---

## Conclusion

The tactical RPG progression system transforms your virtual pet from a passive care simulator into an **active, skill-based game with long-term goals and competitive depth**. The event-sourced architecture ensures every action contributes to both pet care and combat readiness, creating a unique hybrid genre.

This implementation demonstrates production-ready patterns for:
- Complex multi-aggregate business logic
- Game balance configuration and iteration
- Competitive leaderboards with real-time updates
- Combat systems with deterministic outcomes
- Reward loops that drive engagement

The care-to-combat pipeline creates a **self-reinforcing gameplay loop**: good care generates XP, XP unlocks skills, skills enable deeper dungeon runs, dungeons drop loot, loot improves care capacity. Every system feeds into the others.

Ready to turn your pet into a dungeon-crawling legend? Start with Phase 15 and let the theorycrafting begin! ⚔️🐉🔥