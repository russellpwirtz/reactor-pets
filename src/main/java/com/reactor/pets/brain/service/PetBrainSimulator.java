package com.reactor.pets.brain.service;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.brain.config.BrainSimulationConfig;
import com.reactor.pets.brain.model.BrainParameters;
import com.reactor.pets.brain.model.Cell;
import com.reactor.pets.brain.model.CellState;
import com.reactor.pets.brain.model.Grid;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Manages brain simulations for pets with subscription-based lifecycle.
 * Simulations only run when clients are actively watching (WebSocket subscribers).
 */
@Slf4j
@Service
public class PetBrainSimulator {

    private final PetBrainRuleEngine ruleEngine;
    private final PetBrainMapper petBrainMapper;
    private final BrainSimulationConfig config;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    // Map of petId -> Grid
    private final ConcurrentHashMap<String, Grid> petGrids = new ConcurrentHashMap<>();

    // Map of petId -> simulation subscription
    private final ConcurrentHashMap<String, reactor.core.Disposable> simulations =
            new ConcurrentHashMap<>();

    // Map of petId -> current brain parameters (cached even when simulation stopped)
    private final ConcurrentHashMap<String, BrainParameters> currentParameters =
            new ConcurrentHashMap<>();

    // Map of petId -> subscriber count (how many clients are watching)
    private final ConcurrentHashMap<String, AtomicInteger> subscriberCounts =
            new ConcurrentHashMap<>();

    // Map of petId -> scheduled shutdown task
    private final ConcurrentHashMap<String, java.util.concurrent.ScheduledFuture<?>>
            shutdownTasks = new ConcurrentHashMap<>();

    public PetBrainSimulator(
            PetBrainRuleEngine ruleEngine,
            PetBrainMapper petBrainMapper,
            BrainSimulationConfig config) {
        this.ruleEngine = ruleEngine;
        this.petBrainMapper = petBrainMapper;
        this.config = config;

        log.info("PetBrainSimulator initialized with lazy subscription-based lifecycle");
    }

    /**
     * Initialize brain grid for a pet (lazy - does NOT start simulation).
     * Called when first client subscribes or when pet state updates arrive.
     */
    private void ensureGridInitialized(
            String petId,
            int hunger,
            int happiness,
            int health,
            PetStage stage,
            EvolutionPath evolutionPath) {

        if (petGrids.containsKey(petId)) {
            return; // Already initialized
        }

        log.info("Initializing brain grid for pet {}, stage: {}", petId, stage);

        // Calculate initial parameters
        BrainParameters params =
                petBrainMapper.calculateBrainParameters(
                        hunger, happiness, health, stage, evolutionPath);
        currentParameters.put(petId, params);

        // Create grid (but don't start simulation yet)
        Grid grid = new Grid(params.getGridSize(), params.getGridSize());
        petGrids.put(petId, grid);

        // Seed initial pattern based on stage
        seedPatternForStage(grid, stage);

        log.info(
                "Brain grid initialized for pet {}: {}x{} grid (simulation not started)",
                petId, params.getGridSize(), params.getGridSize());
    }

    /**
     * Start simulation for a pet (called when first subscriber connects).
     */
    private void startBrainSimulation(String petId) {
        if (simulations.containsKey(petId)) {
            log.debug("Simulation already running for pet {}", petId);
            return;
        }

        BrainParameters params = currentParameters.get(petId);
        if (params == null) {
            log.warn("Cannot start simulation for pet {} - no parameters", petId);
            return;
        }

        // Update config with current pet parameters
        config.updateFromPetParameters(
                params.getFiringThreshold(),
                params.getDecayFactor(),
                params.getInputLeakage());

        // Update burst threshold from parameters
        ruleEngine.setBurstThresholdMultiplier(params.getBurstThresholdMultiplier());

        // Start simulation
        startSimulation(petId, params.getTickInterval());

        log.info(
                "Brain simulation STARTED for pet {} ({}x{} grid, {}ms tick)",
                petId,
                params.getGridSize(),
                params.getGridSize(),
                params.getTickInterval());
    }

    /**
     * Stop simulation for a pet (called when last subscriber disconnects after grace
     * period).
     */
    private void stopBrainSimulation(String petId) {
        stopSimulation(petId);
        // Keep grid and parameters in memory for quick restart
        log.info("Brain simulation STOPPED for pet {} (grid kept in memory)", petId);
    }

    /**
     * Update pet state and recalculate brain parameters.
     * Called periodically (every 5 seconds) or when significant state changes.
     * Updates cached parameters even if simulation is not running.
     */
    public void updatePetState(
            String petId,
            int hunger,
            int happiness,
            int health,
            PetStage stage,
            EvolutionPath evolutionPath) {

        // Ensure grid exists (but don't start simulation)
        ensureGridInitialized(petId, hunger, happiness, health, stage, evolutionPath);

        Grid grid = petGrids.get(petId);
        if (grid == null) {
            log.warn("Failed to initialize grid for pet {}", petId);
            return;
        }

        // Calculate new parameters
        BrainParameters newParams =
                petBrainMapper.calculateBrainParameters(
                        hunger, happiness, health, stage, evolutionPath);
        BrainParameters oldParams = currentParameters.get(petId);

        // Check if grid size changed (evolution stage transition)
        if (oldParams != null
                && oldParams.getGridSize() != newParams.getGridSize()) {
            log.info(
                    "Pet {} evolved from {}x{} to {}x{}",
                    petId,
                    oldParams.getGridSize(),
                    oldParams.getGridSize(),
                    newParams.getGridSize(),
                    newParams.getGridSize());

            // Recreate grid with new size
            boolean wasRunning = simulations.containsKey(petId);
            stopSimulation(petId);
            petGrids.remove(petId);
            ensureGridInitialized(petId, hunger, happiness, health, stage, evolutionPath);

            // Restart simulation if it was running
            if (wasRunning) {
                startBrainSimulation(petId);
            }
            return;
        }

        // Update cached parameters
        currentParameters.put(petId, newParams);

        // If simulation is running, apply parameters immediately
        if (simulations.containsKey(petId)) {
            config.updateFromPetParameters(
                    newParams.getFiringThreshold(),
                    newParams.getDecayFactor(),
                    newParams.getInputLeakage());

            ruleEngine.setBurstThresholdMultiplier(newParams.getBurstThresholdMultiplier());

            log.debug(
                    "Updated RUNNING brain parameters for pet {}: threshold={}, decay={},"
                            + " leakage={}",
                    petId,
                    newParams.getFiringThreshold(),
                    newParams.getDecayFactor(),
                    newParams.getInputLeakage());
        } else {
            log.debug(
                    "Updated CACHED brain parameters for pet {} (simulation not running)",
                    petId);
        }
    }

    /**
     * Stop brain simulation for a pet (when pet dies).
     */
    public void stopBrain(String petId) {
        stopSimulation(petId);
        petGrids.remove(petId);
        currentParameters.remove(petId);
        subscriberCounts.remove(petId);
        log.info("Brain stopped for pet {}", petId);
    }

    /**
     * Subscribe to cell state updates for a pet's brain.
     * Called when WebSocket client connects.
     * Increments subscriber count and starts simulation if needed.
     *
     * Returns Flux of CellState updates for WebSocket streaming.
     */
    public Flux<List<CellState>> subscribeToBrain(
            String petId,
            int hunger,
            int happiness,
            int health,
            PetStage stage,
            EvolutionPath evolutionPath) {

        // Ensure grid is initialized
        ensureGridInitialized(petId, hunger, happiness, health, stage, evolutionPath);

        // Increment subscriber count
        subscriberCounts
                .computeIfAbsent(petId, k -> new AtomicInteger(0))
                .incrementAndGet();

        // Cancel any pending shutdown
        java.util.concurrent.ScheduledFuture<?> shutdownTask =
                shutdownTasks.remove(petId);
        if (shutdownTask != null) {
            shutdownTask.cancel(false);
            log.info("Cancelled shutdown for pet {} - client reconnected", petId);
        }

        // Start simulation if not already running
        startBrainSimulation(petId);

        Grid grid = petGrids.get(petId);
        if (grid == null) {
            return Flux.empty();
        }

        log.info(
                "Client subscribed to brain for pet {} (subscriber count: {})",
                petId,
                subscriberCounts.get(petId).get());

        // Get current state snapshot to emit immediately for new subscribers
        List<CellState> currentSnapshot =
                grid.getAllCells().stream()
                        .map(Cell::getCurrentState)
                        .collect(Collectors.toList());

        // Merge all cell state updates from all cells in the grid
        List<Flux<CellState>> cellFluxes =
                grid.getAllCells().stream()
                        .map(Cell::getStateUpdates)
                        .collect(Collectors.toList());

        // Create ongoing stream with heartbeat to prevent completion
        // Wrap each cell flux with error handling to prevent one cell from killing the stream
        List<Flux<CellState>> resilientFluxes = cellFluxes.stream()
                .map(flux -> flux
                        .onErrorResume(error -> {
                            log.error("Cell flux error for pet {}: {}", petId, error.getMessage());
                            return Flux.empty();
                        })
                        .doOnComplete(() -> log.debug("Individual cell flux completed for pet {}", petId))
                )
                .collect(Collectors.toList());

        Flux<List<CellState>> ongoingStream = Flux.merge(resilientFluxes)
                .buffer(Duration.ofMillis(50)) // Batch updates every 50ms
                .filter(list -> !list.isEmpty())
                .mergeWith(
                        // Heartbeat: emit empty list every 30 seconds to keep connection alive
                        // This prevents the stream from completing if cells go quiet
                        Flux.interval(Duration.ofSeconds(30))
                                .map(tick -> List.<CellState>of())
                                .filter(list -> !list.isEmpty()) // Filter out empty heartbeats
                )
                .onErrorContinue((error, obj) ->
                        log.error("Error in brain stream for pet {}: {}", petId, error.getMessage()))
                .doOnComplete(() ->
                        log.warn("Brain stream completed unexpectedly for pet {}", petId));

        return Flux.concat(
                        // First, emit current state snapshot immediately
                        Flux.just(currentSnapshot),
                        // Then stream ongoing updates indefinitely
                        ongoingStream)
                .doFinally(
                        signalType -> {
                            log.info("Brain stream finally block triggered for pet {} with signal: {}", petId, signalType);
                            // Client disconnected - decrement subscriber count
                            unsubscribeFromBrain(petId);
                        });
    }

    /**
     * Unsubscribe from brain updates (called when client disconnects).
     * Decrements subscriber count and schedules shutdown if no subscribers remain.
     */
    private void unsubscribeFromBrain(String petId) {
        AtomicInteger count = subscriberCounts.get(petId);
        if (count == null) {
            return;
        }

        int remaining = count.decrementAndGet();
        log.info(
                "Client unsubscribed from brain for pet {} (remaining subscribers: {})",
                petId, remaining);

        if (remaining <= 0) {
            // Schedule shutdown after grace period
            long gracePeriodMs = config.getShutdownGracePeriodMs();
            java.util.concurrent.ScheduledFuture<?> task =
                    scheduler.schedule(
                            () -> {
                                // Double-check subscriber count before stopping
                                AtomicInteger finalCount = subscriberCounts.get(petId);
                                if (finalCount != null && finalCount.get() <= 0) {
                                    log.info(
                                            "Grace period expired for pet {}, stopping"
                                                    + " simulation",
                                            petId);
                                    stopBrainSimulation(petId);
                                    subscriberCounts.remove(petId);
                                    shutdownTasks.remove(petId);
                                }
                            },
                            gracePeriodMs,
                            TimeUnit.MILLISECONDS);

            shutdownTasks.put(petId, task);
            log.info(
                    "Scheduled shutdown for pet {} in {}ms (grace period)",
                    petId, gracePeriodMs);
        }
    }

    private void startSimulation(String petId, int tickInterval) {
        Grid grid = petGrids.get(petId);
        if (grid == null) {
            return;
        }

        // Create tick flux
        Flux<Long> ticker =
                Flux.interval(Duration.ofMillis(tickInterval))
                        .subscribeOn(Schedulers.parallel());

        // Subscribe and run simulation
        reactor.core.Disposable subscription =
                ticker.subscribe(
                        tick -> {
                            // Evaluate all cells
                            List<Cell> allCells = grid.getAllCells();

                            for (Cell cell : allCells) {
                                List<Cell> neighbors = cell.getNeighbors();
                                CellState newState = ruleEngine.evaluateCell(cell, neighbors);
                                cell.emitState(newState);
                            }
                        });

        simulations.put(petId, subscription);
    }

    private void stopSimulation(String petId) {
        reactor.core.Disposable subscription = simulations.remove(petId);
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    private void seedPatternForStage(Grid grid, PetStage stage) {
        int centerX = grid.getWidth() / 2;
        int centerY = grid.getHeight() / 2;
        java.util.Random random = new java.util.Random();

        // Seed multiple activation points based on stage
        // More advanced stages get more initial activation points
        int seedCount = switch (stage) {
            case EGG -> grid.getWidth() / 4; // Sparse for egg
            case BABY -> grid.getWidth() / 3;
            case TEEN -> grid.getWidth() / 2;
            case ADULT -> grid.getWidth() * 3 / 4; // Dense for adult
        };

        // Activate cells in a distributed pattern
        for (int i = 0; i < seedCount; i++) {
            // Create multiple activation points distributed across the grid
            int x;
            int y;

            if (i == 0) {
                // Always activate center
                x = centerX;
                y = centerY;
            } else {
                // Distribute other points across quadrants
                int quadrant = i % 4;
                int offsetX = random.nextInt(grid.getWidth() / 4);
                int offsetY = random.nextInt(grid.getHeight() / 4);

                x = switch (quadrant) {
                    case 0 -> centerX + offsetX; // Top-right
                    case 1 -> centerX - offsetX; // Top-left
                    case 2 -> centerX + offsetX; // Bottom-right
                    case 3 -> centerX - offsetX; // Bottom-left
                    default -> centerX;
                };

                y = switch (quadrant) {
                    case 0, 1 -> centerY - offsetY; // Top half
                    case 2, 3 -> centerY + offsetY; // Bottom half
                    default -> centerY;
                };
            }

            // Ensure coordinates are within bounds
            x = Math.max(0, Math.min(grid.getWidth() - 1, x));
            y = Math.max(0, Math.min(grid.getHeight() - 1, y));

            Cell cell = grid.getCell(x, y);
            CellState current = cell.getCurrentState();

            // Create new state with activation
            // Vary activation strength slightly
            double activation = 0.7 + (random.nextDouble() * 0.3); // 0.7 to 1.0

            CellState activeState =
                    CellState.builder()
                            .cellId(current.getCellId())
                            .x(current.getX())
                            .y(current.getY())
                            .activation(activation)
                            .refractoryCountdown(0)
                            .lastFiredAt(System.currentTimeMillis())
                            .timestamp(System.currentTimeMillis())
                            .isFiring(true)
                            .accumulatedInput(0.0)
                            .dominantDirection(null)
                            .layer(current.getLayer())
                            .cellType(current.getCellType())
                            .neuronPhase(current.getNeuronPhase())
                            .phaseCountdown(0)
                            .burstMode(false)
                            .burstCount(0)
                            .build();

            cell.emitState(activeState);
        }

        log.info("Seeded {} activation points for {} stage ({}x{} grid)",
                seedCount, stage, grid.getWidth(), grid.getHeight());
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down all brain simulations");

        // Cancel all scheduled shutdowns
        shutdownTasks.values().forEach(task -> task.cancel(false));
        shutdownTasks.clear();

        // Stop all simulations
        simulations.values().forEach(reactor.core.Disposable::dispose);
        simulations.clear();

        // Clear state
        petGrids.clear();
        currentParameters.clear();
        subscriberCounts.clear();

        // Shutdown scheduler
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
