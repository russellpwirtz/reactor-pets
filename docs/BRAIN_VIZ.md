# Pet Brain Visualization - Implementation Guide

## Implementation Status

**Last Updated**: 2025-10-30

| Phase | Status | Notes |
|-------|--------|-------|
| **Phase 1: Core Model Extraction** | ✅ Complete | All models ported, zero violations |
| **Phase 2: Rule Engine & Simulator** | ✅ Complete | Refactored architecture, zero violations |
| **Phase 3: Pet Lifecycle Integration** | 🚧 In Progress | Next up |
| **Phase 4: Test Coverage** | ⏳ Pending | - |
| **Phase 5: Frontend Visualization** | ⏳ Pending | - |
| **Phase 6: Polish & Optimization** | ⏳ Pending | - |

**Code Quality**:
- ✅ Zero checkstyle violations
- ✅ Zero SpotBugs issues
- ✅ Clean architecture with phase-specific handlers
- ✅ All acceptance criteria met for Phases 1-2

## Overview

This document outlines the complete implementation plan for integrating neural simulation visualization from `reactor-life` into the `reactor-pets` Tamagotchi project. The brain visualization will provide real-time feedback that reflects the pet's state (hunger, happiness, health) and evolution stage, creating an engaging connection between pet care and neural activity patterns.

## Design Decisions

| Decision | Value | Rationale |
|----------|-------|-----------|
| **Adult Grid Size** | 100x100 | Provides visually rich complexity for mature pets |
| **Parameter Update Frequency** | Every 5 seconds | Balances responsiveness with performance |
| **NEGLECTED Evolution Path** | Visually different | Not "worse", just distinct aesthetic |
| **Configuration Approach** | Spring `application.yml` | Consistent with existing project patterns |
| **Simulation Lifecycle** | Lazy + Subscription-based | Only run simulations when clients are actively viewing |

## Source Reference

All code to port is located in: `../reactor-life/reactor-neural-sim-backend/`

Key files:
- **Models**: `src/main/java/com/reactor/neural/model/`
  - `CellState.java` - Complete cell state with biological properties
  - `CellType.java` - Excitatory/Inhibitory enum
  - `CorticalLayer.java` - Layer structure (L2/3, L4, L5, L6)
  - `NeuronPhase.java` - Hodgkin-Huxley action potential phases
  - `Cell.java` - Reactive cell with Reactor Sinks
  - `Grid.java` - Grid topology with Moore neighborhood
  - `SynapticWeight.java` - Directional weighting
- **Services**: `src/main/java/com/reactor/neural/service/`
  - `RuleEngine.java` - Complete Hodgkin-Huxley neuron firing rules
  - `CellSimulator.java` - Grid simulation orchestration
- **Config**: `src/main/java/com/reactor/neural/config/`
  - `SimulationConfig.java` - Configurable simulation parameters

Frontend visualization (Phase 4):
- `../reactor-life/reactor-neural-sim-frontend/app/components/Visualizer3D.tsx`
- `../reactor-life/reactor-neural-sim-frontend/app/components/OptimizedVisualizer.tsx`

## Key Differences from Original TAMAGOTCHI_BRAIN_INTEGRATION.md

This document refines the original design with a **critical performance optimization**:

| Aspect | Original Design | This Design (BRAIN_VIZ.md) |
|--------|----------------|----------------------------|
| **Simulation Start** | On pet creation | On first WebSocket subscriber |
| **Simulation Stop** | On pet death only | 30s after last subscriber disconnects |
| **Resource Usage** | All pets simulated always | Only viewed pets simulated |
| **Parameter Updates** | Always applied | Cached if not running, applied if running |
| **Grid Lifecycle** | Created and destroyed | Created, paused, destroyed |

**Why This Matters**: With 100 pets in the database, the original design would run 100 concurrent brain simulations. This design runs simulations ONLY for pets being actively viewed (typically 1-5), reducing CPU/memory usage by 95%+.

## Quick Summary

This implementation adds a 3D neural visualization to each pet that reflects its emotional/physical state in real-time through biological neuron simulation patterns. The visualization is **subscription-based and lazy** - brain simulations only run when someone is actively watching.

**Key Features**:
- ✅ Pet hunger/happiness/health drive neural firing patterns
- ✅ Grid complexity scales with evolution stage (20x20 → 100x100)
- ✅ Simulations start ONLY when WebSocket clients connect
- ✅ 30-second grace period before stopping (smooth page refreshes)
- ✅ Parameters cached and updated every 5 seconds (whether running or not)
- ✅ Multiple concurrent brains supported with individual subscriber tracking

**Architecture Flow**:
```
┌─────────────────────────────────────────────────────────────────┐
│ Pet Lifecycle                                                   │
├─────────────────────────────────────────────────────────────────┤
│ Pet Created → Parameters Cached (NO simulation running)         │
│      │                                                           │
│      ├──→ Pet state changes every 5s                            │
│      │    └──→ Update CACHED parameters (not applied yet)       │
│      │                                                           │
│      ↓                                                           │
│ ┌──────────────────────────────────────────────────────┐        │
│ │ WebSocket Subscribe Event                            │        │
│ │  - Client connects to /pets/{petId}/brain            │        │
│ │  - Grid initialized (if first subscriber)            │        │
│ │  - Simulation STARTED                                │        │
│ │  - Subscriber count: 0 → 1                           │        │
│ │  - Cell updates streamed to client                   │        │
│ └──────────────────────────────────────────────────────┘        │
│      │                                                           │
│      ├──→ Pet state changes (simulation running)                │
│      │    └──→ Apply parameters IMMEDIATELY                     │
│      │                                                           │
│      ↓                                                           │
│ ┌──────────────────────────────────────────────────────┐        │
│ │ WebSocket Unsubscribe Event                          │        │
│ │  - Client disconnects                                │        │
│ │  - Subscriber count: 1 → 0                           │        │
│ │  - Schedule shutdown in 30s                          │        │
│ └──────────────────────────────────────────────────────┘        │
│      │                                                           │
│      ├──→ If client reconnects within 30s:                      │
│      │    └──→ Cancel shutdown, keep simulation running         │
│      │                                                           │
│      ├──→ If 30s expires:                                       │
│      │    └──→ Simulation STOPPED (grid kept in memory)         │
│      │                                                           │
│      ↓                                                           │
│ Pet Dies → Simulation stopped, grid deleted, cleanup complete   │
└─────────────────────────────────────────────────────────────────┘
```

## Simulation Lifecycle Strategy

**Problem**: Running brain simulations for all pets continuously wastes CPU/memory when no one is watching.

**Solution**: Lazy initialization with subscription counting:

1. **WebSocket Subscribe**: When a client connects to `/pets/{petId}/brain`:
   - Check if simulation exists
   - If not, initialize grid and start simulation
   - Increment subscriber count
   - Stream cell updates

2. **WebSocket Unsubscribe**: When client disconnects:
   - Decrement subscriber count
   - If count reaches 0, stop simulation after 30-second grace period
   - Keep grid in memory for quick restart

3. **Grace Period**: 30 seconds before stopping allows:
   - Page refreshes without restart
   - Quick re-connections
   - Smooth UX

4. **Pet State Updates**: When pet state changes (hunger, health, etc.):
   - Update cached parameters
   - If simulation is running, apply immediately
   - If simulation is stopped, parameters apply on next start

## Pet Attribute → Brain Parameter Mappings

### Core Mappings

| Pet Attribute | Brain Impact | Formula |
|---------------|-------------|---------|
| **Hunger** | Firing threshold (excitability) | `threshold = 2.5 - (hunger / 100.0) * 1.5` <br> Range: 1.0 (starving) to 2.5 (satisfied) |
| **Happiness** | Synchronization/Input leakage | `inputLeakage = 0.05 + (happiness / 100.0) * 0.1` <br> Range: 0.05 to 0.15 |
| **Health** | Overall vibrancy/Decay | `decayFactor = 0.85 + (health / 100.0) * 0.1` <br> Range: 0.85 to 0.95 |
| **Age** | Pattern complexity | Affects burst thresholds and layer activation |

### Stage-Specific Grid Configurations

| Stage | Grid Size | Layers | Tick Interval | Characteristics |
|-------|-----------|--------|---------------|-----------------|
| **EGG** | 20×20 (400 cells) | L2/3 only | 150ms | Growth pattern from center, minimal firing |
| **BABY** | 35×35 (1,225 cells) | L2/3, L4 | 120ms | Simple waves, basic firing patterns |
| **TEEN** | 50×50 (2,500 cells) | L2/3, L4, L5, L6 | 100ms | Complex patterns, bursting capability |
| **ADULT** | 100×100 (10,000 cells) | All layers, full complexity | 80ms | Sophisticated cortical rhythms |

### Evolution Path Visual Differences

**HEALTHY Path**:
- Smooth synchronized wave patterns
- Calm color gradients
- Efficient sparse firing
- Grid: 100×100

**NEGLECTED Path**:
- Chaotic desynchronized firing
- Sharp color contrasts
- Frequent bursting
- Grid: 100×100 (same size, different dynamics)

## Implementation Phases

---

## Phase 1: Core Model Extraction ✅ COMPLETED

**Goal**: Port all neural simulation models into reactor-pets project

### Files Created

```
src/main/java/com/reactor/pets/brain/model/
├── CellState.java           - Complete cell state with biological properties
├── CellType.java            - Excitatory/Inhibitory enum
├── CorticalLayer.java       - Layer structure (L2/3, L4, L5, L6)
├── NeuronPhase.java         - Hodgkin-Huxley action potential phases
├── SynapticWeight.java      - Directional weighting
├── Cell.java                - Reactive cell with Reactor Sinks
├── Grid.java                - Grid topology with Moore neighborhood
└── BrainParameters.java     - Pet-to-brain parameter mapping DTO

src/main/java/com/reactor/pets/brain/service/
└── PetBrainMapper.java      - Maps pet stats → brain parameters
```

### Key Implementation Details

- **PetBrainMapper** calculates brain parameters from pet attributes:
  - Hunger → Firing threshold (1.0-2.5)
  - Happiness → Input leakage (0.05-0.15)
  - Health → Decay factor (0.85-0.95)
  - Stage → Grid size (20×20 to 100×100) and tick interval
  - Evolution path → Burst threshold multiplier (NEGLECTED: 1.8, HEALTHY: 2.5)

### Quality Metrics
- ✅ Zero checkstyle violations
- ✅ Zero SpotBugs issues
- ✅ All acceptance criteria met

---

## Phase 2: Rule Engine and Simulation Service ✅ COMPLETED

**Goal**: Port RuleEngine and create PetBrainSimulator service

### Files Created

```
src/main/java/com/reactor/pets/brain/service/
├── PetBrainRuleEngine.java      - Hodgkin-Huxley neuron firing logic (refactored)
└── PetBrainSimulator.java       - Subscription-based brain simulation manager

src/main/java/com/reactor/pets/brain/config/
└── BrainSimulationConfig.java   - Spring configuration for brain parameters
```

### Architecture Highlights

**PetBrainRuleEngine** - Refactored for clean code:
- Main `evaluateCell()` orchestrates weighted neighbor inputs with E/I balance
- Phase-specific handlers (6 methods, each < 50 lines):
  - `handleRestingPhase()` - Firing threshold checks & burst detection
  - `handleDepolarizingPhase()` - Fast rise (action potential peak)
  - `handleRepolarizingPhase()` - Falling phase with burst continuation
  - `handleHyperpolarizedPhase()` - Absolute refractory period
  - `handleRecoveringPhase()` - Relative refractory (elevated threshold)
  - `handleBurstingPhase()` - Layer 5 pyramidal burst mode
- Internal `PhaseState` class encapsulates mutable state

**PetBrainSimulator** - Subscription-based lifecycle:
- Lazy initialization: grids created but simulations don't start until WebSocket subscribers connect
- Concurrent HashMap architecture supports multiple pet brains
- **Subscriber tracking**: Counts active WebSocket viewers per pet
- **Grace period**: 30-second delay before stopping (smooth page refreshes)
- **Parameter caching**: Updates cached even when simulation paused
- **Grid resizing**: Handles evolution transitions (20×20 → 100×100)

### Configuration (application.yml)

```yaml
brain:
  simulation:
    firing-threshold: 1.5
    refractory-period: 5
    decay-factor: 0.95
    input-leakage: 0.1
    base-weight: 1.0
    paused: false
    shutdown-grace-period-ms: 30000  # 30 seconds
```

### Quality Improvements

**Refactoring** (2025-10-30):
- Method length violation eliminated: `evaluateNeuronPhase()` split into 6 focused handlers
- Internal `PhaseState` class encapsulates mutable state transitions
- SpotBugs fixes: Removed duplicate switch clauses, removed unused HashMap
- **Result**: Zero checkstyle violations, zero SpotBugs issues

---

## Phase 3: Integration with Pet Lifecycle (Backend Integration)

**Estimated Time**: 2-3 hours
**Goal**: Wire brain simulation into pet lifecycle events

### Tasks

1. **Create `BrainLifecycleSaga.java`**:
   - Listen to pet events and manage brain lifecycle
   - Handle: `PetCreatedEvent`, `TimePassedEvent`, `PetEvolvedEvent`, `PetDiedEvent`

   ```java
   package com.reactor.pets.saga;

   import com.reactor.pets.aggregate.EvolutionPath;
   import com.reactor.pets.aggregate.PetStage;
   import com.reactor.pets.brain.service.PetBrainSimulator;
   import com.reactor.pets.event.PetCreatedEvent;
   import com.reactor.pets.event.PetDiedEvent;
   import com.reactor.pets.event.PetEvolvedEvent;
   import com.reactor.pets.event.TimePassedEvent;
   import com.reactor.pets.query.GetPetStatusQuery;
   import com.reactor.pets.query.PetStatusRepository;
   import lombok.extern.slf4j.Slf4j;
   import org.axonframework.eventhandling.EventHandler;
   import org.axonframework.queryhandling.QueryGateway;
   import org.springframework.stereotype.Component;

   import java.util.concurrent.CompletableFuture;
   import java.util.concurrent.Executors;
   import java.util.concurrent.ScheduledExecutorService;
   import java.util.concurrent.TimeUnit;

   @Slf4j
   @Component
   public class BrainLifecycleSaga {

       private final PetBrainSimulator brainSimulator;
       private final QueryGateway queryGateway;
       private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

       public BrainLifecycleSaga(
               PetBrainSimulator brainSimulator,
               QueryGateway queryGateway) {
           this.brainSimulator = brainSimulator;
           this.queryGateway = queryGateway;
       }

       @EventHandler
       public void on(PetCreatedEvent event) {
           log.info("Pet created: {} - brain will initialize on first client connection", event.getPetId());

           // Schedule periodic updates every 5 seconds
           // This updates CACHED parameters even if simulation isn't running
           scheduler.scheduleAtFixedRate(
               () -> updateBrainFromPetState(event.getPetId()),
               5, 5, TimeUnit.SECONDS
           );
       }

       @EventHandler
       public void on(PetEvolvedEvent event) {
           log.info("Pet {} evolved to {}, updating brain", event.getPetId(), event.getNewStage());

           // Force immediate update on evolution
           updateBrainFromPetState(event.getPetId());
       }

       @EventHandler
       public void on(PetDiedEvent event) {
           log.info("Pet {} died, stopping brain", event.getPetId());
           brainSimulator.stopBrain(event.getPetId());
       }

       private void updateBrainFromPetState(String petId) {
           // Query current pet status
           queryGateway.query(
               new GetPetStatusQuery(petId),
               com.reactor.pets.query.PetStatusRepository.PetStatus.class
           ).thenAccept(status -> {
               if (status != null && status.isAlive()) {
                   brainSimulator.updatePetState(
                       petId,
                       status.getHunger(),
                       status.getHappiness(),
                       status.getHealth(),
                       status.getStage(),
                       status.getEvolutionPath()
                   );
               }
           }).exceptionally(ex -> {
               log.error("Failed to update brain for pet {}", petId, ex);
               return null;
           });
       }
   }
   ```

2. **Create WebSocket endpoint for brain streaming**:
   - Create `BrainWebSocketController.java`

   ```java
   package com.reactor.pets.api.controller;

   import com.reactor.pets.brain.model.CellState;
   import com.reactor.pets.brain.service.PetBrainSimulator;
   import com.reactor.pets.query.GetPetStatusQuery;
   import com.reactor.pets.query.PetStatusRepository;
   import lombok.extern.slf4j.Slf4j;
   import org.axonframework.queryhandling.QueryGateway;
   import org.springframework.messaging.handler.annotation.DestinationVariable;
   import org.springframework.messaging.handler.annotation.MessageMapping;
   import org.springframework.stereotype.Controller;
   import reactor.core.publisher.Flux;
   import reactor.core.publisher.Mono;

   import java.util.List;

   @Slf4j
   @Controller
   public class BrainWebSocketController {

       private final PetBrainSimulator brainSimulator;
       private final QueryGateway queryGateway;

       public BrainWebSocketController(
               PetBrainSimulator brainSimulator,
               QueryGateway queryGateway) {
           this.brainSimulator = brainSimulator;
           this.queryGateway = queryGateway;
       }

       @MessageMapping("/pets/{petId}/brain")
       public Flux<List<CellState>> streamBrainActivity(@DestinationVariable String petId) {
           log.info("Client subscribed to brain stream for pet: {}", petId);

           // Query pet status to get current state
           return Mono.fromFuture(
               queryGateway.query(
                   new GetPetStatusQuery(petId),
                   PetStatusRepository.PetStatus.class
               )
           ).flatMapMany(status -> {
               if (status == null || !status.isAlive()) {
                   log.warn("Cannot stream brain for pet {} - not found or dead", petId);
                   return Flux.empty();
               }

               // Subscribe to brain with current pet state
               // This will start simulation if needed and track subscriber count
               return brainSimulator.subscribeToBrain(
                   petId,
                   status.getHunger(),
                   status.getHappiness(),
                   status.getHealth(),
                   status.getStage(),
                   status.getEvolutionPath()
               );
           });
       }
   }
   ```

3. **Update POM dependencies** (if needed):
   - Ensure WebSocket and Reactor dependencies are present
   - Verify Lombok is available

### Acceptance Criteria

- [ ] Brain does NOT initialize automatically when pet is created
- [ ] Brain initializes ONLY when first WebSocket client connects
- [ ] Brain parameters update every 5 seconds (cached if not running, applied if running)
- [ ] Brain simulation starts when first client subscribes
- [ ] Subscriber count tracks concurrent viewers
- [ ] Brain simulation stops 30 seconds after last client disconnects
- [ ] Brain grid remains in memory during grace period for quick restart
- [ ] Brain simulation updates on evolution stage transitions (if running)
- [ ] Brain stops and clears when pet dies
- [ ] WebSocket endpoint streams cell updates
- [ ] Multiple concurrent pet brains work correctly
- [ ] Logs clearly show when simulations START, STOP, and parameter updates

### Files to Create

```
src/main/java/com/reactor/pets/saga/BrainLifecycleSaga.java
src/main/java/com/reactor/pets/api/controller/BrainWebSocketController.java
```

---

## Phase 4: Test Coverage (Backend Testing)

**Estimated Time**: 3-4 hours
**Goal**: Achieve comprehensive test coverage for brain simulation

### Tasks

1. **Create `PetBrainMapperTest.java`**:
   ```java
   package com.reactor.pets.brain.service;

   import com.reactor.pets.aggregate.EvolutionPath;
   import com.reactor.pets.aggregate.PetStage;
   import com.reactor.pets.brain.model.BrainParameters;
   import org.junit.jupiter.api.BeforeEach;
   import org.junit.jupiter.api.Test;

   import static org.junit.jupiter.api.Assertions.*;

   class PetBrainMapperTest {

       private PetBrainMapper mapper;

       @BeforeEach
       void setUp() {
           mapper = new PetBrainMapper();
       }

       @Test
       void calculateBrainParameters_EggStage_ShouldReturnSmallGrid() {
           BrainParameters params = mapper.calculateBrainParameters(
               50, 50, 100, PetStage.EGG, null);

           assertEquals(20, params.getGridSize());
           assertEquals(150, params.getTickInterval());
       }

       @Test
       void calculateBrainParameters_AdultStage_ShouldReturnLargeGrid() {
           BrainParameters params = mapper.calculateBrainParameters(
               50, 50, 100, PetStage.ADULT, EvolutionPath.HEALTHY);

           assertEquals(100, params.getGridSize());
           assertEquals(80, params.getTickInterval());
       }

       @Test
       void calculateBrainParameters_HighHunger_ShouldLowerThreshold() {
           // High hunger = low threshold (more excitable)
           BrainParameters params = mapper.calculateBrainParameters(
               100, 50, 50, PetStage.BABY, null);

           assertEquals(1.0, params.getFiringThreshold(), 0.01);
       }

       @Test
       void calculateBrainParameters_LowHunger_ShouldRaiseThreshold() {
           // Low hunger = high threshold (calm)
           BrainParameters params = mapper.calculateBrainParameters(
               0, 50, 50, PetStage.BABY, null);

           assertEquals(2.5, params.getFiringThreshold(), 0.01);
       }

       @Test
       void calculateBrainParameters_HighHappiness_ShouldIncreaseLeakage() {
           BrainParameters params = mapper.calculateBrainParameters(
               50, 100, 50, PetStage.BABY, null);

           assertEquals(0.15, params.getInputLeakage(), 0.01);
       }

       @Test
       void calculateBrainParameters_LowHealth_ShouldLowerDecayFactor() {
           BrainParameters params = mapper.calculateBrainParameters(
               50, 50, 0, PetStage.BABY, null);

           assertEquals(0.85, params.getDecayFactor(), 0.01);
       }

       @Test
       void calculateBrainParameters_NeglectedPath_ShouldLowerBurstThreshold() {
           BrainParameters params = mapper.calculateBrainParameters(
               50, 50, 50, PetStage.ADULT, EvolutionPath.NEGLECTED);

           assertEquals(1.8, params.getBurstThresholdMultiplier(), 0.01);
       }

       @Test
       void calculateBrainParameters_HealthyPath_ShouldMaintainBurstThreshold() {
           BrainParameters params = mapper.calculateBrainParameters(
               50, 50, 50, PetStage.ADULT, EvolutionPath.HEALTHY);

           assertEquals(2.5, params.getBurstThresholdMultiplier(), 0.01);
       }
   }
   ```

2. **Create `PetBrainSimulatorTest.java`**:
   - Test initialization
   - Test parameter updates
   - Test grid resizing on evolution
   - Test cleanup on pet death
   - Mock dependencies: `PetBrainRuleEngine`, `PetBrainMapper`, `BrainSimulationConfig`

3. **Create `BrainLifecycleSagaTest.java`**:
   - Test brain initialization on `PetCreatedEvent`
   - Test brain update on `PetEvolvedEvent`
   - Test brain shutdown on `PetDiedEvent`
   - Use Mockito for mocking

4. **Create integration test `BrainIntegrationTest.java`**:
   - Test full flow: create pet → brain initializes → parameters update → pet evolves → grid resizes
   - Use `@SpringBootTest` with embedded Axon

### Acceptance Criteria

- [ ] All unit tests pass with > 80% coverage
- [ ] Integration test verifies full lifecycle
- [ ] Tests cover edge cases (null evolution path, extreme stat values)
- [ ] Tests verify concurrent brain simulations
- [ ] Mock tests are isolated and fast

### Files to Create

```
src/test/java/com/reactor/pets/brain/service/PetBrainMapperTest.java
src/test/java/com/reactor/pets/brain/service/PetBrainSimulatorTest.java
src/test/java/com/reactor/pets/saga/BrainLifecycleSagaTest.java
src/test/java/com/reactor/pets/integration/BrainIntegrationTest.java
```

---

## Phase 5: Frontend Visualization (3D Brain Viewer)

**Estimated Time**: 4-5 hours
**Goal**: Port Three.js visualization and integrate with pet UI

### Tasks

1. **Copy `Visualizer3D.tsx`** from reactor-life:
   - Source: `../reactor-life/reactor-neural-sim-frontend/app/components/Visualizer3D.tsx`
   - Destination: `reactor-pets-frontend/src/components/brain/BrainVisualizer3D.tsx`
   - Modifications:
     - Update WebSocket connection to `/pets/{petId}/brain`
     - Accept `petId` as prop
     - Adjust camera positioning for variable grid sizes
     - Preserve color coding (white/cyan firing, green/blue active states)

2. **Create `BrainVisualizerPanel.tsx`**:
   ```tsx
   'use client';

   import React, { useState } from 'react';
   import { Card } from '@/components/ui/card';
   import BrainVisualizer3D from './BrainVisualizer3D';

   interface BrainVisualizerPanelProps {
     petId: string;
     stage: string;
   }

   export default function BrainVisualizerPanel({ petId, stage }: BrainVisualizerPanelProps) {
     const [isFullscreen, setIsFullscreen] = useState(false);

     return (
       <Card className="p-4">
         <div className="flex justify-between items-center mb-2">
           <h3 className="text-lg font-semibold">Brain Activity</h3>
           <button
             onClick={() => setIsFullscreen(!isFullscreen)}
             className="text-sm text-blue-500 hover:underline"
           >
             {isFullscreen ? 'Minimize' : 'Fullscreen'}
           </button>
         </div>

         <div className={isFullscreen ? 'fixed inset-0 z-50 bg-black' : 'h-96'}>
           <BrainVisualizer3D
             petId={petId}
             stage={stage}
             fullscreen={isFullscreen}
           />
         </div>

         <div className="mt-2 text-xs text-gray-500">
           <p>Stage: {stage}</p>
           <p className="mt-1">
             <span className="inline-block w-3 h-3 bg-white border border-gray-300 mr-1"></span>
             Firing
             <span className="inline-block w-3 h-3 bg-green-400 ml-3 mr-1"></span>
             Active
             <span className="inline-block w-3 h-3 bg-gray-700 ml-3 mr-1"></span>
             Resting
           </p>
         </div>
       </Card>
     );
   }
   ```

3. **Integrate into pet detail page**:
   - Update `reactor-pets-frontend/src/app/pets/[id]/page.tsx`
   - Add `BrainVisualizerPanel` to layout
   - Place below pet stats or in a new tab

4. **Add WebSocket connection logic**:
   - Use existing WebSocket infrastructure or create new hook
   - Handle reconnection logic
   - Parse `CellState[]` batches

5. **Optimize rendering**:
   - Use instanced meshes for performance (copy from reactor-life)
   - Implement frustum culling for large grids
   - Add loading state while connecting

### Acceptance Criteria

- [ ] 3D visualization renders correctly for all stages
- [ ] Colors match biological coding (white/cyan firing, green/blue active)
- [ ] WebSocket streams cell updates without lag
- [ ] Fullscreen mode works correctly
- [ ] Performance maintains 30+ FPS for 100×100 grid
- [ ] Camera auto-adjusts for different grid sizes
- [ ] Visualization reflects pet state changes within 5 seconds

### Files to Create

```
reactor-pets-frontend/src/components/brain/BrainVisualizer3D.tsx
reactor-pets-frontend/src/components/brain/BrainVisualizerPanel.tsx
reactor-pets-frontend/src/hooks/useBrainWebSocket.ts (if needed)
```

### Files to Modify

```
reactor-pets-frontend/src/app/pets/[id]/page.tsx
```

---

## Phase 6: Polish and Optimization (Final Touches)

**Estimated Time**: 2-3 hours
**Goal**: Performance tuning, documentation, and final polish

### Tasks

1. **Performance Optimization**:
   - Profile brain simulation CPU usage
   - Ensure multiple concurrent brains don't degrade performance
   - Add circuit breaker for simulation failures
   - Implement backpressure handling in WebSocket stream

2. **Add monitoring metrics**:
   - Track active brain simulations
   - Monitor grid evaluation time
   - Log parameter updates

3. **Documentation**:
   - Add JavaDoc to all public methods
   - Create API documentation for WebSocket endpoints
   - Update project README with brain visualization features

4. **Visual polish**:
   - Fine-tune color gradients for better visual appeal
   - Add subtle animations for grid transitions
   - Improve loading states

5. **Configuration**:
   - Make all thresholds configurable via `application.yml`
   - Add feature flag to enable/disable brain visualization
   - Allow grid size overrides for testing

### Acceptance Criteria

- [ ] 10 concurrent brain simulations maintain 60 FPS
- [ ] Memory usage is stable over 1 hour
- [ ] All public APIs have JavaDoc
- [ ] README includes brain visualization section
- [ ] Configuration is well-documented
- [ ] No console errors or warnings

---

## Technical Reference

### Color Coding (Preserve from reactor-life)

| Neuron State | Excitatory Color | Inhibitory Color | Height |
|--------------|------------------|------------------|--------|
| Firing | White (1, 1, 1) | Cyan (0.4, 1, 1) | 2.0 |
| Active | Green gradient | Blue gradient | 0.1 - 1.5 |
| Resting | Dark gray (0.1, 0.1, 0.1) | Slight blue tint | 0.1 |
| Hyperpolarized | Very dark | Very dark blue | 0.1 |

### Layer Brightness Modulation

- L2/3: 100% brightness (superficial)
- L4: 90% brightness
- L5: 75% brightness
- L6: 60% brightness (deep)

### Performance Targets

| Stage | Grid Size | Cells | Target FPS |
|-------|-----------|-------|-----------|
| EGG | 20×20 | 400 | 120+ FPS |
| BABY | 35×35 | 1,225 | 90+ FPS |
| TEEN | 50×50 | 2,500 | 60+ FPS |
| ADULT | 100×100 | 10,000 | 30+ FPS |

---

## Success Metrics

**Technical**:
- ✓ Brain simulation maintains target FPS for each stage
- ✓ Parameter updates complete within 50ms of pet state change
- ✓ No memory leaks over 1 hour continuous operation
- ✓ Test coverage > 80% for brain simulation code

**Visual Appeal**:
- ✓ Clear visual difference between HEALTHY and NEGLECTED pets
- ✓ Firing events are dramatic and noticeable
- ✓ Color coding is intuitive and visually appealing

**Integration**:
- ✓ Brain activity meaningfully reflects pet state (not random)
- ✓ Stage transitions are smooth and visually interesting
- ✓ Brain visualization enhances gameplay without overshadowing core Tamagotchi mechanics

**Resource Efficiency** (NEW):
- ✓ Zero simulations running with 0 active viewers
- ✓ Only viewed pets consume CPU/memory
- ✓ Grace period allows smooth page refreshes without restart
- ✓ Can handle 100+ pets in database with only 1-5 active simulations

---

## Implementation Notes for Claude Code Sessions

When implementing each phase, pay special attention to:

1. **Phase 2**: The subscription tracking logic in `PetBrainSimulator` is critical. Test thoroughly:
   - Multiple concurrent subscribers to same pet
   - Subscribe → Unsubscribe → Quick Resubscribe (before grace period)
   - Grace period expiration with no re-subscription

2. **Phase 3**: The `BrainLifecycleSaga` should NOT start simulations automatically. Log statements should clearly differentiate:
   - "Parameters CACHED (not running)"
   - "Parameters APPLIED (running)"
   - "Simulation STARTED"
   - "Simulation STOPPED"

3. **Phase 4**: Frontend WebSocket connection should handle reconnection gracefully. The grace period is designed to make this seamless.

4. **Testing**: Pay special attention to the subscriber counting logic - off-by-one errors here will cause either premature shutdowns or leaked simulations.

---

**Document Version**: 1.1
**Date**: 2025-10-30
**Author**: Russell
**Status**: Ready for Implementation
**Key Change**: Added subscription-based lifecycle management (v1.0 → v1.1)
