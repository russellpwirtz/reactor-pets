package com.reactor.pets.brain.service;

import static org.junit.jupiter.api.Assertions.*;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.brain.model.BrainParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PetBrainMapperTest {

    private PetBrainMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PetBrainMapper();
    }

    @Test
    void testCalculateBrainParameters_EggStage() {
        // Arrange
        int hunger = 50;
        int happiness = 50;
        int health = 100;
        PetStage stage = PetStage.EGG;
        EvolutionPath path = null;

        // Act
        BrainParameters params =
                mapper.calculateBrainParameters(hunger, happiness, health, stage, path);

        // Assert
        assertNotNull(params);
        assertEquals(20, params.getGridSize(), "EGG stage should have 20x20 grid");
        assertEquals(150, params.getTickInterval(), "EGG stage should have 150ms tick interval");
        assertEquals(1.0, params.getBaseActivity(), "Health 100 should give activity 1.0");
        assertTrue(
                params.getFiringThreshold() >= 1.0 && params.getFiringThreshold() <= 2.5,
                "Firing threshold should be in valid range");
    }

    @Test
    void testCalculateBrainParameters_BabyStage() {
        // Arrange
        int hunger = 30;
        int happiness = 70;
        int health = 80;
        PetStage stage = PetStage.BABY;
        EvolutionPath path = null;

        // Act
        BrainParameters params =
                mapper.calculateBrainParameters(hunger, happiness, health, stage, path);

        // Assert
        assertEquals(35, params.getGridSize(), "BABY stage should have 35x35 grid");
        assertEquals(120, params.getTickInterval(), "BABY stage should have 120ms tick interval");
        assertEquals(0.8, params.getBaseActivity(), 0.01, "Health 80 should give activity 0.8");
    }

    @Test
    void testCalculateBrainParameters_TeenStage() {
        // Arrange
        int hunger = 40;
        int happiness = 60;
        int health = 90;
        PetStage stage = PetStage.TEEN;
        EvolutionPath path = EvolutionPath.HEALTHY;

        // Act
        BrainParameters params =
                mapper.calculateBrainParameters(hunger, happiness, health, stage, path);

        // Assert
        assertEquals(50, params.getGridSize(), "TEEN stage should have 50x50 grid");
        assertEquals(100, params.getTickInterval(), "TEEN stage should have 100ms tick interval");
        assertEquals(2.5, params.getBurstThresholdMultiplier(), 0.01, "HEALTHY path should have 2.5 burst threshold");
    }

    @Test
    void testCalculateBrainParameters_AdultStage() {
        // Arrange
        int hunger = 20;
        int happiness = 80;
        int health = 100;
        PetStage stage = PetStage.ADULT;
        EvolutionPath path = EvolutionPath.HEALTHY;

        // Act
        BrainParameters params =
                mapper.calculateBrainParameters(hunger, happiness, health, stage, path);

        // Assert
        assertEquals(100, params.getGridSize(), "ADULT stage should have 100x100 grid");
        assertEquals(80, params.getTickInterval(), "ADULT stage should have 80ms tick interval");
    }

    @Test
    void testCalculateBrainParameters_NeglectedPath() {
        // Arrange
        int hunger = 80;
        int happiness = 20;
        int health = 50;
        PetStage stage = PetStage.ADULT;
        EvolutionPath path = EvolutionPath.NEGLECTED;

        // Act
        BrainParameters params =
                mapper.calculateBrainParameters(hunger, happiness, health, stage, path);

        // Assert
        assertEquals(
                1.8,
                params.getBurstThresholdMultiplier(),
                0.01,
                "NEGLECTED path should have 1.8 burst threshold (easier bursting)");
    }

    @Test
    void testFiringThreshold_HungerEffect() {
        // Arrange
        PetStage stage = PetStage.ADULT;
        int happiness = 50;
        int health = 100;

        // Low hunger (well fed) = high threshold (calm)
        BrainParameters wellFed =
                mapper.calculateBrainParameters(0, happiness, health, stage, null);

        // High hunger (starving) = low threshold (excitable)
        BrainParameters starving =
                mapper.calculateBrainParameters(100, happiness, health, stage, null);

        // Assert
        assertTrue(
                wellFed.getFiringThreshold() > starving.getFiringThreshold(),
                "Well-fed pet should have higher firing threshold (calmer)");
        assertEquals(
                2.5, wellFed.getFiringThreshold(), 0.01, "Hunger 0 should give threshold 2.5");
        assertEquals(
                1.0, starving.getFiringThreshold(), 0.01, "Hunger 100 should give threshold 1.0");
    }

    @Test
    void testInputLeakage_HappinessEffect() {
        // Arrange
        PetStage stage = PetStage.ADULT;
        int hunger = 50;
        int health = 100;

        // Low happiness = low leakage
        BrainParameters unhappy = mapper.calculateBrainParameters(hunger, 0, health, stage, null);

        // High happiness = high leakage (more synchronization)
        BrainParameters happy = mapper.calculateBrainParameters(hunger, 100, health, stage, null);

        // Assert
        assertTrue(
                happy.getInputLeakage() > unhappy.getInputLeakage(),
                "Happy pet should have higher input leakage");
        assertEquals(
                0.05, unhappy.getInputLeakage(), 0.01, "Happiness 0 should give leakage 0.05");
        assertEquals(
                0.15, happy.getInputLeakage(), 0.01, "Happiness 100 should give leakage 0.15");
    }

    @Test
    void testDecayFactor_HealthEffect() {
        // Arrange
        PetStage stage = PetStage.ADULT;
        int hunger = 50;
        int happiness = 50;

        // Low health = low decay factor
        BrainParameters unhealthy = mapper.calculateBrainParameters(hunger, happiness, 0, stage, null);

        // High health = high decay factor
        BrainParameters healthy =
                mapper.calculateBrainParameters(hunger, happiness, 100, stage, null);

        // Assert
        assertTrue(
                healthy.getDecayFactor() > unhealthy.getDecayFactor(),
                "Healthy pet should have higher decay factor");
        assertEquals(
                0.85, unhealthy.getDecayFactor(), 0.01, "Health 0 should give decay 0.85");
        assertEquals(
                0.95, healthy.getDecayFactor(), 0.01, "Health 100 should give decay 0.95");
    }
}
