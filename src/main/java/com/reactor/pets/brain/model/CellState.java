package com.reactor.pets.brain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CellState {
    @JsonProperty("cellId")
    private String cellId;

    @JsonProperty("x")
    private int x;

    @JsonProperty("y")
    private int y;

    @JsonProperty("activation")
    private double activation; // 0.0 to 1.0

    @JsonProperty("refractoryCountdown")
    private int refractoryCountdown;

    @JsonProperty("lastFiredAt")
    private long lastFiredAt;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("isFiring")
    private boolean isFiring;

    // Phase 2 additions
    @JsonProperty("accumulatedInput")
    private double accumulatedInput; // Sum of weighted neighbor inputs for this tick

    @JsonProperty("dominantDirection")
    private String dominantDirection; // Direction of strongest input (N,S,E,W,NE,NW,SE,SW)

    // Phase 6 additions: Biological neuron properties
    @JsonProperty("layer")
    private CorticalLayer layer; // Cortical layer (L2/3, L4, L5, L6)

    @JsonProperty("cellType")
    private CellType cellType; // Excitatory or inhibitory

    @JsonProperty("neuronPhase")
    private NeuronPhase neuronPhase; // Current phase in action potential waveform

    @JsonProperty("phaseCountdown")
    private int phaseCountdown; // Ticks remaining in current phase

    @JsonProperty("burstMode")
    private boolean burstMode; // True if neuron is in bursting mode

    @JsonProperty("burstCount")
    private int burstCount; // Remaining spikes in burst
}
