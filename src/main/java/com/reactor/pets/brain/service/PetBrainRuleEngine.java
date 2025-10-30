package com.reactor.pets.brain.service;

import com.reactor.pets.brain.config.BrainSimulationConfig;
import com.reactor.pets.brain.model.Cell;
import com.reactor.pets.brain.model.CellState;
import com.reactor.pets.brain.model.CellType;
import com.reactor.pets.brain.model.CorticalLayer;
import com.reactor.pets.brain.model.NeuronPhase;
import com.reactor.pets.brain.model.SynapticWeight;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implements biologically-realistic neuron firing rules for pet brain simulation.
 * Enhanced with Hodgkin-Huxley action potential dynamics, bursting behavior,
 * and excitatory/inhibitory neuron types.
 */
@Slf4j
@Service
public class PetBrainRuleEngine {

    private final BrainSimulationConfig config;

    // Default burst threshold (can be overridden by pet parameters)
    private double burstThresholdMultiplier = 2.5;
    private static final int BURST_SPIKE_COUNT = 3;

    @Autowired
    public PetBrainRuleEngine(BrainSimulationConfig config) {
        this.config = config;
    }

    /**
     * Set burst threshold multiplier from pet parameters.
     * Lower values (1.8) make bursting easier (chaotic NEGLECTED pets).
     * Higher values (2.5) make bursting harder (calm HEALTHY pets).
     */
    public void setBurstThresholdMultiplier(double multiplier) {
        this.burstThresholdMultiplier = multiplier;
    }

    /**
     * Evaluate cell state with Hodgkin-Huxley dynamics, bursting, and E/I balance.
     */
    public CellState evaluateCell(Cell cell, List<Cell> neighbors) {
        CellState current = cell.getCurrentState();

        // Skip evaluation if simulation is paused
        if (config.isPaused()) {
            return current;
        }

        // Calculate weighted sum of neighbor activations with E/I polarity
        double totalInput = 0.0;
        String dominantDirection = null;
        double maxDirectionalInput = 0.0;

        for (Cell neighbor : neighbors) {
            CellState neighborState = neighbor.getCurrentState();

            // Determine direction from neighbor to this cell
            String direction =
                    SynapticWeight.getDirection(
                            neighbor.getX(), neighbor.getY(), cell.getX(), cell.getY());

            // Apply synaptic weight based on direction
            double weight = config.getWeightForDirection(direction);

            // Apply cell type polarity (excitatory = +1, inhibitory = -1)
            double polarity = neighbor.getCellType().getOutputPolarity();
            double weightedInput = neighborState.getActivation() * weight * polarity;

            totalInput += weightedInput;

            // Track dominant input direction
            if (Math.abs(weightedInput) > maxDirectionalInput) {
                maxDirectionalInput = Math.abs(weightedInput);
                dominantDirection = direction;
            }
        }

        // Apply layer-specific firing threshold multiplier
        CorticalLayer layer =
                (cell.getLayer() != null) ? cell.getLayer() : CorticalLayer.LAYER_2_3;
        double effectiveThreshold =
                config.getFiringThreshold() / layer.getFiringMultiplier();

        // Hodgkin-Huxley action potential state machine
        return evaluateNeuronPhase(
                cell, current, totalInput, effectiveThreshold, dominantDirection);
    }

    /**
     * Hodgkin-Huxley action potential state machine.
     * Dispatches to phase-specific handlers.
     */
    private CellState evaluateNeuronPhase(
            Cell cell,
            CellState current,
            double totalInput,
            double threshold,
            String dominantDirection) {
        // Null-safe initialization
        NeuronPhase currentPhase =
                (current.getNeuronPhase() != null)
                        ? current.getNeuronPhase()
                        : NeuronPhase.RESTING;

        PhaseState state =
                new PhaseState(
                        currentPhase,
                        current.getPhaseCountdown(),
                        current.getActivation(),
                        current.isBurstMode(),
                        current.getBurstCount(),
                        false);

        // Dispatch to phase-specific handler
        switch (currentPhase) {
            case RESTING:
                state = handleRestingPhase(cell, state, totalInput, threshold);
                break;
            case DEPOLARIZING:
                state = handleDepolarizingPhase(state);
                break;
            case REPOLARIZING:
                state = handleRepolarizingPhase(cell, state);
                break;
            case HYPERPOLARIZED:
                state = handleHyperpolarizedPhase(state);
                break;
            case RECOVERING:
                state = handleRecoveringPhase(cell, state, totalInput, threshold);
                break;
            case BURSTING:
                state = handleBurstingPhase(cell, state);
                break;
            default:
                log.warn(
                        "Unknown neuron phase: {} for cell {}",
                        currentPhase,
                        cell.getCellId());
                break;
        }

        // Clamp activation to valid range
        double clampedActivation = Math.max(-0.1, Math.min(1.0, state.activation));

        // Build new state
        return CellState.builder()
                .cellId(current.getCellId())
                .x(current.getX())
                .y(current.getY())
                .activation(clampedActivation)
                .refractoryCountdown(0)
                .lastFiredAt(
                        state.isFiring ? System.currentTimeMillis() : current.getLastFiredAt())
                .timestamp(System.currentTimeMillis())
                .isFiring(state.isFiring)
                .accumulatedInput(totalInput)
                .dominantDirection(dominantDirection)
                .layer(current.getLayer())
                .cellType(current.getCellType())
                .neuronPhase(state.phase)
                .phaseCountdown(state.phaseCountdown)
                .burstMode(state.burstMode)
                .burstCount(state.burstCount)
                .build();
    }

    private PhaseState handleRestingPhase(
            Cell cell, PhaseState state, double totalInput, double threshold) {
        boolean shouldFire = totalInput > threshold;

        // Layer 5 burst detection (strong excitatory input)
        boolean shouldBurst =
                shouldFire
                        && cell.getLayer() == CorticalLayer.LAYER_5
                        && cell.getCellType() == CellType.EXCITATORY
                        && totalInput > (threshold * burstThresholdMultiplier);

        if (shouldFire) {
            state.phase = NeuronPhase.DEPOLARIZING;
            state.phaseCountdown = NeuronPhase.DEPOLARIZING.getTypicalDuration();
            state.activation = 1.0;
            state.isFiring = true;

            if (shouldBurst) {
                state.burstMode = true;
                state.burstCount = BURST_SPIKE_COUNT - 1;
                log.debug(
                        "Cell {} entering BURST mode! Input: {}",
                        cell.getCellId(),
                        totalInput);
            }

            log.debug("Cell {} fired! Phase: RESTING -> DEPOLARIZING", cell.getCellId());
        } else {
            // Not firing - decay and apply input leakage
            double decayedActivation = state.activation * config.getDecayFactor();
            double leakedInput = Math.max(0, totalInput) * config.getInputLeakage();
            state.activation = Math.min(decayedActivation + leakedInput, 0.99);
            state.phase = NeuronPhase.RESTING;
            state.phaseCountdown = 0;
        }

        return state;
    }

    private PhaseState handleDepolarizingPhase(PhaseState state) {
        state.phaseCountdown--;
        state.activation = 1.0; // Peak
        state.isFiring = true;

        if (state.phaseCountdown == 0) {
            state.phase = NeuronPhase.REPOLARIZING;
            state.phaseCountdown = NeuronPhase.REPOLARIZING.getTypicalDuration();
        }

        return state;
    }

    private PhaseState handleRepolarizingPhase(Cell cell, PhaseState state) {
        state.phaseCountdown--;
        double progress =
                1.0
                        - ((double) state.phaseCountdown
                                / NeuronPhase.REPOLARIZING.getTypicalDuration());
        state.activation = 1.0 - (progress * 1.1); // Fall from 1.0 to -0.1

        if (state.phaseCountdown == 0) {
            if (state.burstMode && state.burstCount > 0) {
                // Continue burst
                state.phase = NeuronPhase.BURSTING;
                state.phaseCountdown = NeuronPhase.BURSTING.getTypicalDuration();
                state.activation = 1.0;
                state.burstCount = Math.max(0, state.burstCount - 1);
                state.isFiring = true;
                log.debug(
                        "Cell {} burst spike {} of {}",
                        cell.getCellId(),
                        (BURST_SPIKE_COUNT - state.burstCount + 1),
                        BURST_SPIKE_COUNT);
            } else {
                // Normal transition to hyperpolarized
                state.phase = NeuronPhase.HYPERPOLARIZED;
                state.phaseCountdown = NeuronPhase.HYPERPOLARIZED.getTypicalDuration();
                state.burstMode = false;
                state.burstCount = 0;
            }
        }

        return state;
    }

    private PhaseState handleHyperpolarizedPhase(PhaseState state) {
        state.phaseCountdown--;
        state.activation = -0.1;

        if (state.phaseCountdown == 0) {
            state.phase = NeuronPhase.RECOVERING;
            state.phaseCountdown = NeuronPhase.RECOVERING.getTypicalDuration();
        }

        return state;
    }

    private PhaseState handleRecoveringPhase(
            Cell cell, PhaseState state, double totalInput, double threshold) {
        if (state.phaseCountdown > 0) {
            // Still counting down recovery
            state.phaseCountdown--;
            double recoveryProgress =
                    1.0
                            - ((double) state.phaseCountdown
                                    / NeuronPhase.RECOVERING.getTypicalDuration());
            state.activation = -0.1 + (recoveryProgress * 0.1); // -0.1 to 0.0

            if (state.phaseCountdown == 0) {
                state.phase = NeuronPhase.RESTING;
                state.activation = 0.0;
            }
        } else {
            // Recovery countdown finished - can fire with stronger input
            double relativeThreshold = threshold * 1.5;
            boolean canFireRecovering = totalInput > relativeThreshold;

            if (canFireRecovering) {
                state.phase = NeuronPhase.DEPOLARIZING;
                state.phaseCountdown = NeuronPhase.DEPOLARIZING.getTypicalDuration();
                state.activation = 1.0;
                state.isFiring = true;
                log.debug(
                        "Cell {} fired during recovery! Input: {}",
                        cell.getCellId(),
                        totalInput);
            } else {
                state.phase = NeuronPhase.RESTING;
                state.activation = 0.0;
                state.phaseCountdown = 0;
            }
        }

        return state;
    }

    private PhaseState handleBurstingPhase(Cell cell, PhaseState state) {
        state.phaseCountdown--;
        state.activation = 1.0; // Stay at peak during burst
        state.isFiring = true;

        if (state.phaseCountdown == 0) {
            if (state.burstMode && state.burstCount > 0) {
                // Continue burst
                state.phaseCountdown = NeuronPhase.BURSTING.getTypicalDuration();
                state.burstCount = Math.max(0, state.burstCount - 1);
                log.debug(
                        "Cell {} continuing burst: {} spikes remaining",
                        cell.getCellId(),
                        state.burstCount);
            } else {
                // Burst finished
                state.phase = NeuronPhase.HYPERPOLARIZED;
                state.phaseCountdown = NeuronPhase.HYPERPOLARIZED.getTypicalDuration();
                state.burstMode = false;
                state.burstCount = 0;
                log.debug("Cell {} burst complete", cell.getCellId());
            }
        }

        return state;
    }

    /**
     * Internal class to hold mutable phase state during evaluation.
     */
    @Data
    @AllArgsConstructor
    private static final class PhaseState {
        NeuronPhase phase;
        int phaseCountdown;
        double activation;
        boolean burstMode;
        int burstCount;
        boolean isFiring;
    }
}
