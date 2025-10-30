package com.reactor.pets.brain.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * A reactive cell that maintains state and communicates with neighbors.
 * Phase 6: Enhanced with biological neuron properties (layer, cell type).
 */
public class Cell {
    @Getter
    private final String cellId;

    @Getter
    private final int x;

    @Getter
    private final int y;

    // Phase 6: Biological properties
    @Getter
    private final CorticalLayer layer;

    @Getter
    private final CellType cellType;

    // Sink for publishing state updates
    private final Sinks.Many<CellState> stateSink;

    // Public flux that others can subscribe to
    @Getter
    private final Flux<CellState> stateUpdates;

    // Current state snapshot
    @Getter
    private volatile CellState currentState;

    // References to neighbor cells (populated after grid initialization)
    private final List<Cell> neighbors = new ArrayList<>(8);

    public Cell(int x, int y, CorticalLayer layer, CellType cellType) {
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.cellType = cellType;
        this.cellId = String.format("cell-%d-%d", x, y);

        // Create a multicast sink (multiple subscribers allowed)
        this.stateSink = Sinks.many().multicast().onBackpressureBuffer();

        // Expose as hot flux
        this.stateUpdates = stateSink.asFlux();

        // Initialize with resting state
        this.currentState = CellState.builder()
            .cellId(cellId)
            .x(x)
            .y(y)
            .activation(0.0)
            .refractoryCountdown(0)
            .lastFiredAt(0L)
            .timestamp(System.currentTimeMillis())
            .isFiring(false)
            .layer(layer)
            .cellType(cellType)
            .neuronPhase(NeuronPhase.RESTING)
            .phaseCountdown(0)
            .burstMode(false)
            .burstCount(0)
            .build();
    }

    /**
     * Emit a new state through the sink with error handling.
     * Phase 4: Enhanced with proper error handling and retry logic.
     */
    public void emitState(CellState newState) {
        if (newState == null) {
            // Defensive programming - prevent null states
            return;
        }

        this.currentState = newState;

        // Try to emit with proper error handling
        Sinks.EmitResult result = this.stateSink.tryEmitNext(newState);

        // Handle backpressure or failure scenarios
        switch (result) {
            case OK:
                // Successfully emitted
                break;
            case FAIL_OVERFLOW:
                // Buffer overflow - log but don't crash
                // This is expected under high load
                break;
            case FAIL_NON_SERIALIZED:
                // Concurrent access - retry once
                this.stateSink.tryEmitNext(newState);
                break;
            case FAIL_CANCELLED:
            case FAIL_TERMINATED:
                // Sink is terminated - this is expected during shutdown
                break;
            case FAIL_ZERO_SUBSCRIBER:
                // No subscribers yet - this is fine
                break;
            default:
                // Unknown failure - this shouldn't happen
                break;
        }
    }

    /**
     * Add a neighbor cell for observation.
     */
    public void addNeighbor(Cell neighbor) {
        if (neighbors.size() < 8 && !neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
        }
    }

    /**
     * Get all neighbors for rule evaluation.
     */
    public List<Cell> getNeighbors() {
        return new ArrayList<>(neighbors);
    }
}
