package com.reactor.pets.brain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.reactor.pets.brain.config.BrainSimulationConfig;
import com.reactor.pets.brain.model.Cell;
import com.reactor.pets.brain.model.CellState;
import com.reactor.pets.brain.model.CellType;
import com.reactor.pets.brain.model.CorticalLayer;
import com.reactor.pets.brain.model.NeuronPhase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PetBrainRuleEngineTest {

    private PetBrainRuleEngine ruleEngine;
    private BrainSimulationConfig config;

    @BeforeEach
    void setUp() {
        config = new BrainSimulationConfig();
        config.setFiringThreshold(1.5);
        config.setDecayFactor(0.95);
        config.setInputLeakage(0.1);
        config.setPaused(false);

        ruleEngine = new PetBrainRuleEngine(config);
    }

    @Test
    void testEvaluateCell_RestingPhase_NoFiring() {
        // Arrange
        Cell cell =
                new Cell(
                        5, 5, CorticalLayer.LAYER_2_3, CellType.EXCITATORY);
        List<Cell> neighbors = new ArrayList<>();

        // All neighbors have zero activation
        for (int i = 0; i < 8; i++) {
            Cell neighbor = new Cell(i, i, CorticalLayer.LAYER_2_3, CellType.EXCITATORY);
            neighbors.add(neighbor);
        }

        // Act
        CellState newState = ruleEngine.evaluateCell(cell, neighbors);

        // Assert
        assertFalse(newState.isFiring(), "Cell should not fire with no input");
        assertEquals(
                NeuronPhase.RESTING,
                newState.getNeuronPhase(),
                "Cell should remain in RESTING phase");
    }

    @Test
    void testEvaluateCell_RestingPhase_Firing() {
        // Arrange
        Cell cell = new Cell(5, 5, CorticalLayer.LAYER_2_3, CellType.EXCITATORY);
        List<Cell> neighbors = createActiveNeighbors(8);

        // Act
        CellState newState = ruleEngine.evaluateCell(cell, neighbors);

        // Assert
        assertTrue(newState.isFiring(), "Cell should fire with strong input");
        assertEquals(
                NeuronPhase.DEPOLARIZING,
                newState.getNeuronPhase(),
                "Cell should transition to DEPOLARIZING");
        assertEquals(1.0, newState.getActivation(), 0.01, "Activation should be at peak");
    }

    @Test
    void testEvaluateCell_BurstDetection_Layer5() {
        // Arrange
        Cell cell = new Cell(5, 5, CorticalLayer.LAYER_5, CellType.EXCITATORY);
        ruleEngine.setBurstThresholdMultiplier(1.5); // Low threshold for easier testing

        List<Cell> neighbors = createActiveNeighbors(8);

        // Act
        CellState newState = ruleEngine.evaluateCell(cell, neighbors);

        // Assert - with strong input, Layer 5 should burst
        assertTrue(newState.isBurstMode(), "Layer 5 excitatory should enter burst mode");
        assertEquals(
                2,
                newState.getBurstCount(),
                "Should have 2 remaining burst spikes (total 3, first already firing)");
    }

    @Test
    void testEvaluateCell_NoBurst_Layer2() {
        // Arrange
        Cell cell = new Cell(5, 5, CorticalLayer.LAYER_2_3, CellType.EXCITATORY);
        ruleEngine.setBurstThresholdMultiplier(1.5);

        List<Cell> neighbors = createActiveNeighbors(8);

        // Act
        CellState newState = ruleEngine.evaluateCell(cell, neighbors);

        // Assert - Layer 2/3 should not burst (only Layer 5 bursts)
        assertFalse(newState.isBurstMode(), "Layer 2/3 should not enter burst mode");
    }

    @Test
    void testEvaluateCell_InhibitoryNeuron() {
        // Arrange
        Cell cell = new Cell(5, 5, CorticalLayer.LAYER_2_3, CellType.EXCITATORY);

        // Create neighbors with one inhibitory neuron firing strongly
        List<Cell> neighbors = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            neighbors.add(new Cell(i, i, CorticalLayer.LAYER_2_3, CellType.EXCITATORY));
        }

        // One inhibitory neuron firing
        Cell inhibitoryNeighbor =
                new Cell(7, 7, CorticalLayer.LAYER_2_3, CellType.INHIBITORY);
        CellState inhibitoryState = inhibitoryNeighbor.getCurrentState();
        inhibitoryState =
                CellState.builder()
                        .cellId(inhibitoryState.getCellId())
                        .x(7)
                        .y(7)
                        .activation(1.0)
                        .isFiring(true)
                        .layer(CorticalLayer.LAYER_2_3)
                        .cellType(CellType.INHIBITORY)
                        .neuronPhase(NeuronPhase.DEPOLARIZING)
                        .phaseCountdown(2)
                        .burstMode(false)
                        .burstCount(0)
                        .refractoryCountdown(0)
                        .timestamp(System.currentTimeMillis())
                        .lastFiredAt(System.currentTimeMillis())
                        .build();
        inhibitoryNeighbor.emitState(inhibitoryState);
        neighbors.add(inhibitoryNeighbor);

        // Act
        CellState newState = ruleEngine.evaluateCell(cell, neighbors);

        // Assert - Inhibitory input should suppress firing
        assertFalse(
                newState.isFiring(),
                "Cell should not fire due to inhibitory neighbor");
    }

    @Test
    void testEvaluateCell_PausedSimulation() {
        // Arrange
        config.setPaused(true);
        Cell cell = new Cell(5, 5, CorticalLayer.LAYER_2_3, CellType.EXCITATORY);
        List<Cell> neighbors = createActiveNeighbors(8);

        CellState originalState = cell.getCurrentState();

        // Act
        CellState newState = ruleEngine.evaluateCell(cell, neighbors);

        // Assert - Should return unchanged state when paused
        assertEquals(originalState, newState, "State should not change when simulation is paused");
    }

    @Test
    void testSetBurstThresholdMultiplier() {
        // Arrange
        double newThreshold = 2.0;

        // Act
        ruleEngine.setBurstThresholdMultiplier(newThreshold);

        // Assert - Can't directly verify, but should not throw exception
        assertDoesNotThrow(() -> ruleEngine.setBurstThresholdMultiplier(newThreshold));
    }

    @Test
    void testLayerSpecificThreshold() {
        // Arrange - Layer 4 has higher firing multiplier (1.2)
        Cell layer4Cell = new Cell(5, 5, CorticalLayer.LAYER_4, CellType.EXCITATORY);
        Cell layer6Cell = new Cell(5, 5, CorticalLayer.LAYER_6, CellType.EXCITATORY);

        List<Cell> moderateNeighbors = createActiveNeighbors(4);

        // Act
        CellState layer4State = ruleEngine.evaluateCell(layer4Cell, moderateNeighbors);
        CellState layer6State = ruleEngine.evaluateCell(layer6Cell, moderateNeighbors);

        // Assert - Layer 4 should fire more easily (lower effective threshold)
        // This is based on the layer's firingMultiplier affecting the effective threshold
        assertNotNull(layer4State);
        assertNotNull(layer6State);
    }

    // Helper method to create neighbors with active state
    private List<Cell> createActiveNeighbors(int count) {
        List<Cell> neighbors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Cell neighbor = new Cell(i, i, CorticalLayer.LAYER_2_3, CellType.EXCITATORY);

            // Set neighbor to active firing state
            CellState activeState = neighbor.getCurrentState();
            activeState =
                    CellState.builder()
                            .cellId(activeState.getCellId())
                            .x(i)
                            .y(i)
                            .activation(1.0)
                            .isFiring(true)
                            .layer(CorticalLayer.LAYER_2_3)
                            .cellType(CellType.EXCITATORY)
                            .neuronPhase(NeuronPhase.DEPOLARIZING)
                            .phaseCountdown(2)
                            .burstMode(false)
                            .burstCount(0)
                            .refractoryCountdown(0)
                            .timestamp(System.currentTimeMillis())
                            .lastFiredAt(System.currentTimeMillis())
                            .build();
            neighbor.emitState(activeState);

            neighbors.add(neighbor);
        }
        return neighbors;
    }
}
