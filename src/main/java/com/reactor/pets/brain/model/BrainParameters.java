package com.reactor.pets.brain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrainParameters {
    private double firingThreshold;     // 1.0 to 2.5
    private double decayFactor;          // 0.85 to 0.95
    private double inputLeakage;         // 0.05 to 0.15
    private double baseActivity;         // 0.0 to 1.0 (from health)
    private int gridSize;                // Based on PetStage
    private int tickInterval;            // Milliseconds between ticks
    private double burstThresholdMultiplier; // 1.8 to 2.5
}
