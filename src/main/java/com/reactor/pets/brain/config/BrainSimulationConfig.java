package com.reactor.pets.brain.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "brain.simulation")
@Data
public class BrainSimulationConfig {

    // Core parameters (will be overridden by pet state)
    private double firingThreshold = 1.5;
    private int refractoryPeriod = 5;
    private double decayFactor = 0.95;
    private double inputLeakage = 0.1;
    private double baseWeight = 1.0;
    private boolean paused = false;

    // Grace period before stopping simulation (milliseconds)
    private long shutdownGracePeriodMs = 30000L; // Default 30 seconds

    // Synaptic weights for directional propagation
    private Map<String, Double> synapticWeights = createDefaultWeights();

    private static Map<String, Double> createDefaultWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("N", 1.0);
        weights.put("S", 1.0);
        weights.put("E", 1.0);
        weights.put("W", 1.0);
        weights.put("NE", 0.9);
        weights.put("NW", 0.9);
        weights.put("SE", 0.9);
        weights.put("SW", 0.9);
        return weights;
    }

    public double getWeightForDirection(String direction) {
        return synapticWeights.getOrDefault(direction, baseWeight);
    }

    /**
     * Update parameters from pet state.
     * Called when applying pet-specific brain parameters to running simulation.
     */
    public void updateFromPetParameters(
            double firingThreshold,
            double decayFactor,
            double inputLeakage) {
        this.firingThreshold = firingThreshold;
        this.decayFactor = decayFactor;
        this.inputLeakage = inputLeakage;
    }
}
