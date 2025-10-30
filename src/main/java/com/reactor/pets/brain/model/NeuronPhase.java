package com.reactor.pets.brain.model;

/**
 * Neuron phase representing the Hodgkin-Huxley action potential waveform.
 * Models the realistic spike dynamics of biological neurons.
 */
public enum NeuronPhase {
    /**
     * Resting state: Below threshold, accumulating input.
     * Activation near 0.0, membrane at resting potential.
     */
    RESTING(0.0, 0),

    /**
     * Depolarizing: Rapid rise phase of action potential.
     * Fast sodium channels open, activation shoots to 1.0.
     * Duration: 1-2 ticks (extremely fast)
     */
    DEPOLARIZING(1.0, 2),

    /**
     * Repolarizing: Falling phase of action potential.
     * Potassium channels open, sodium channels close.
     * Activation falls from 1.0 back toward 0.0.
     * Duration: 3-4 ticks
     */
    REPOLARIZING(0.4, 3),

    /**
     * Hyperpolarized: Undershoot below resting potential.
     * Prevents immediate re-firing (absolute refractory period).
     * Activation goes slightly negative (-0.1).
     * Duration: 2-3 ticks
     */
    HYPERPOLARIZED(-0.1, 2),

    /**
     * Recovering: Slow return to resting potential.
     * Relative refractory period - can fire but requires stronger input.
     * Activation gradually returns from negative to 0.0.
     * Duration: 3-4 ticks
     */
    RECOVERING(0.0, 3),

    /**
     * Bursting: Special high-frequency firing mode.
     * Active in Layer 5 pyramidal neurons under strong stimulation.
     * Rapid succession of spikes without full hyperpolarization.
     */
    BURSTING(1.0, 1);

    private final double baseActivation; // Typical activation during this phase
    private final int typicalDuration; // Typical duration in ticks

    NeuronPhase(double baseActivation, int typicalDuration) {
        this.baseActivation = baseActivation;
        this.typicalDuration = typicalDuration;
    }

    public double getBaseActivation() {
        return baseActivation;
    }

    public int getTypicalDuration() {
        return typicalDuration;
    }

    /**
     * Check if this phase allows firing.
     * Only RESTING and RECOVERING phases can initiate a new spike.
     */
    public boolean canFire() {
        return this == RESTING || this == RECOVERING;
    }

    /**
     * Check if this phase represents active spiking.
     */
    public boolean isSpiking() {
        return this == DEPOLARIZING || this == BURSTING;
    }
}
