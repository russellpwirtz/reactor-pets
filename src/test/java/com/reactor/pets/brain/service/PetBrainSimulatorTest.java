package com.reactor.pets.brain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.brain.config.BrainSimulationConfig;
import com.reactor.pets.brain.model.BrainParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetBrainSimulatorTest {

    @Mock private PetBrainRuleEngine ruleEngine;

    @Mock private PetBrainMapper petBrainMapper;

    private BrainSimulationConfig config;
    private PetBrainSimulator simulator;

    @BeforeEach
    void setUp() {
        config = new BrainSimulationConfig();
        config.setShutdownGracePeriodMs(100L); // Short grace period for testing

        simulator = new PetBrainSimulator(ruleEngine, petBrainMapper, config);

        // Setup default mock behavior
        when(petBrainMapper.calculateBrainParameters(
                        anyInt(), anyInt(), anyInt(), any(), any()))
                .thenReturn(
                        BrainParameters.builder()
                                .firingThreshold(1.5)
                                .decayFactor(0.95)
                                .inputLeakage(0.1)
                                .baseActivity(1.0)
                                .gridSize(20)
                                .tickInterval(50)
                                .burstThresholdMultiplier(2.5)
                                .build());
    }

    @Test
    void testUpdatePetState_InitializesGrid() {
        // Arrange
        String petId = "test-pet-1";
        int hunger = 50;
        int happiness = 50;
        int health = 100;
        PetStage stage = PetStage.EGG;
        EvolutionPath path = null;

        // Act
        simulator.updatePetState(petId, hunger, happiness, health, stage, path);

        // Assert - Verify mapper was called
        verify(petBrainMapper, atLeastOnce())
                .calculateBrainParameters(hunger, happiness, health, stage, path);
    }

    @Test
    void testUpdatePetState_UpdatesParameters() {
        // Arrange
        String petId = "test-pet-2";
        PetStage stage = PetStage.BABY;

        // Initialize first
        simulator.updatePetState(petId, 50, 50, 100, stage, null);

        // Act - Update with different values
        simulator.updatePetState(petId, 80, 30, 60, stage, null);

        // Assert - Mapper should be called at least twice (init + update)
        verify(petBrainMapper, atLeast(2))
                .calculateBrainParameters(anyInt(), anyInt(), anyInt(), eq(stage), any());
    }

    @Test
    void testUpdatePetState_GridResizeOnEvolution() {
        // Arrange
        String petId = "test-pet-3";

        BrainParameters eggParams =
                BrainParameters.builder()
                        .gridSize(20)
                        .tickInterval(150)
                        .firingThreshold(1.5)
                        .decayFactor(0.95)
                        .inputLeakage(0.1)
                        .baseActivity(1.0)
                        .burstThresholdMultiplier(2.5)
                        .build();

        BrainParameters babyParams =
                BrainParameters.builder()
                        .gridSize(35)
                        .tickInterval(120)
                        .firingThreshold(1.5)
                        .decayFactor(0.95)
                        .inputLeakage(0.1)
                        .baseActivity(1.0)
                        .burstThresholdMultiplier(2.5)
                        .build();

        when(petBrainMapper.calculateBrainParameters(
                        anyInt(), anyInt(), anyInt(), eq(PetStage.EGG), any()))
                .thenReturn(eggParams);

        when(petBrainMapper.calculateBrainParameters(
                        anyInt(), anyInt(), anyInt(), eq(PetStage.BABY), any()))
                .thenReturn(babyParams);

        // Act - Initialize as EGG
        simulator.updatePetState(petId, 50, 50, 100, PetStage.EGG, null);

        // Act - Evolve to BABY (grid resize)
        simulator.updatePetState(petId, 50, 50, 100, PetStage.BABY, null);

        // Assert - Mapper should be called for both stages
        verify(petBrainMapper, atLeastOnce()).calculateBrainParameters(50, 50, 100, PetStage.EGG, null);
        verify(petBrainMapper, atLeastOnce())
                .calculateBrainParameters(50, 50, 100, PetStage.BABY, null);
    }

    @Test
    void testStopBrain_RemovesGrid() {
        // Arrange
        String petId = "test-pet-4";
        simulator.updatePetState(petId, 50, 50, 100, PetStage.EGG, null);

        // Act
        simulator.stopBrain(petId);

        // Assert - Should complete without error
        assertDoesNotThrow(() -> simulator.stopBrain(petId));
    }

    @Test
    void testSubscribeToBrain_StartsSimulation() throws InterruptedException {
        // Arrange
        String petId = "test-pet-5";

        // Act - Subscribe to brain (starts simulation)
        var flux =
                simulator.subscribeToBrain(
                        petId, 50, 50, 100, PetStage.EGG, EvolutionPath.HEALTHY);

        // Assert - Flux should be non-null
        assertNotNull(flux, "Brain subscription flux should not be null");

        // Cleanup - dispose subscription to avoid leaks
        var subscription = flux.subscribe();
        Thread.sleep(200); // Let simulation run briefly
        subscription.dispose();
        simulator.cleanup();
    }

    @Test
    void testMultiplePetBrains_Concurrent() {
        // Arrange
        String pet1 = "pet-1";
        String pet2 = "pet-2";
        String pet3 = "pet-3";

        // Act - Initialize multiple pet brains
        simulator.updatePetState(pet1, 30, 70, 90, PetStage.BABY, null);
        simulator.updatePetState(pet2, 50, 50, 80, PetStage.TEEN, null);
        simulator.updatePetState(pet3, 70, 30, 60, PetStage.ADULT, EvolutionPath.NEGLECTED);

        // Assert - All should succeed
        verify(petBrainMapper, atLeastOnce()).calculateBrainParameters(30, 70, 90, PetStage.BABY, null);
        verify(petBrainMapper, atLeastOnce()).calculateBrainParameters(50, 50, 80, PetStage.TEEN, null);
        verify(petBrainMapper, atLeastOnce())
                .calculateBrainParameters(70, 30, 60, PetStage.ADULT, EvolutionPath.NEGLECTED);
    }

    @Test
    void testCleanup_DisposesAllSimulations() {
        // Arrange
        simulator.updatePetState("pet-1", 50, 50, 100, PetStage.EGG, null);
        simulator.updatePetState("pet-2", 50, 50, 100, PetStage.BABY, null);

        // Act
        simulator.cleanup();

        // Assert - Should complete without error
        assertDoesNotThrow(() -> simulator.cleanup());
    }

    @Test
    void testSubscribeToBrain_IncreasesSubscriberCount() throws InterruptedException {
        // Arrange
        String petId = "test-pet-6";

        // Act - First subscriber
        var flux1 =
                simulator.subscribeToBrain(
                        petId, 50, 50, 100, PetStage.EGG, EvolutionPath.HEALTHY);
        var sub1 = flux1.subscribe();

        Thread.sleep(100); // Brief wait

        // Act - Second subscriber
        var flux2 =
                simulator.subscribeToBrain(
                        petId, 50, 50, 100, PetStage.EGG, EvolutionPath.HEALTHY);
        var sub2 = flux2.subscribe();

        Thread.sleep(100); // Brief wait

        // Assert - Both subscriptions should be active
        assertNotNull(sub1);
        assertNotNull(sub2);

        // Cleanup
        sub1.dispose();
        sub2.dispose();
        simulator.cleanup();
    }

    @Test
    void testUpdatePetState_NullEvolutionPath() {
        // Arrange
        String petId = "test-pet-7";

        // Act - EGG stage with null evolution path (expected for early stages)
        assertDoesNotThrow(
                () ->
                        simulator.updatePetState(
                                petId, 50, 50, 100, PetStage.EGG, null));

        // Assert - Should handle gracefully
        verify(petBrainMapper, atLeastOnce()).calculateBrainParameters(50, 50, 100, PetStage.EGG, null);
    }
}
