package com.reactor.pets.brain.service;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.brain.model.BrainParameters;
import org.springframework.stereotype.Service;

@Service
public class PetBrainMapper {

    /**
     * Calculate brain simulation parameters from pet attributes.
     *
     * @param hunger 0-100
     * @param happiness 0-100
     * @param health 0-100
     * @param stage PetStage
     * @param evolutionPath EvolutionPath (can be null for EGG/BABY)
     * @return BrainParameters configured for this pet state
     */
    public BrainParameters calculateBrainParameters(
            int hunger, int happiness, int health,
            PetStage stage, EvolutionPath evolutionPath) {

        // Base activity from health (0.0 to 1.0)
        double baseActivity = health / 100.0;

        // Firing threshold - hunger makes neurons more excitable
        // Low hunger (well fed) = high threshold (calm, 2.5)
        // High hunger (starving) = low threshold (excitable, 1.0)
        double firingThreshold = 2.5 - (hunger / 100.0 * 1.5);

        // Synchronization from happiness
        // High happiness = more synchronization (higher leakage)
        double inputLeakage = 0.05 + (happiness / 100.0 * 0.1);

        // Decay factor from health
        double decayFactor = 0.85 + (baseActivity * 0.1);

        // Grid size and tick interval based on stage
        int gridSize;
        int tickInterval;
        switch (stage) {
            case BABY:
                gridSize = 35;
                tickInterval = 120;
                break;
            case TEEN:
                gridSize = 50;
                tickInterval = 100;
                break;
            case ADULT:
                gridSize = 100;
                tickInterval = 80;
                break;
            case EGG:
            default:
                // EGG and any unknown stages use smallest grid
                gridSize = 20;
                tickInterval = 150;
                break;
        }

        // Burst threshold based on evolution path
        double burstThresholdMultiplier = 2.5; // Default HEALTHY
        if (evolutionPath == EvolutionPath.NEGLECTED) {
            burstThresholdMultiplier = 1.8; // Easier bursting (chaotic)
        }

        return BrainParameters.builder()
            .firingThreshold(firingThreshold)
            .decayFactor(decayFactor)
            .inputLeakage(inputLeakage)
            .baseActivity(baseActivity)
            .gridSize(gridSize)
            .tickInterval(tickInterval)
            .burstThresholdMultiplier(burstThresholdMultiplier)
            .build();
    }
}
