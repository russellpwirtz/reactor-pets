package com.reactor.pets.brain.model;

import java.util.Random;

/**
 * Neuron type: excitatory or inhibitory.
 * Biological ratio is approximately 80% excitatory, 20% inhibitory.
 */
public enum CellType {
    /**
     * Excitatory neurons (pyramidal cells, spiny stellate)
     * - Send positive activation to neighbors
     * - ~80% of cortical neurons
     * - Longer-range connections
     */
    EXCITATORY(0.8, 1.0),

    /**
     * Inhibitory interneurons (GABAergic)
     * - Send negative activation to neighbors (suppress firing)
     * - ~20% of cortical neurons
     * - Local connections, provide stability
     * - Essential for pattern formation and oscillations
     */
    INHIBITORY(0.2, -1.0);

    private final double proportion; // Expected proportion in population
    private final double outputPolarity; // +1.0 for excitatory, -1.0 for inhibitory

    CellType(double proportion, double outputPolarity) {
        this.proportion = proportion;
        this.outputPolarity = outputPolarity;
    }

    public double getProportion() {
        return proportion;
    }

    /**
     * Get output polarity: +1.0 for excitatory, -1.0 for inhibitory.
     * Applied as multiplier to activation when computing neighbor input.
     */
    public double getOutputPolarity() {
        return outputPolarity;
    }

    /**
     * Randomly assign cell type based on biological proportions.
     * Layer 4 has higher proportion of inhibitory neurons (fast-spiking interneurons).
     *
     * @param layer Cortical layer
     * @param random Random number generator
     * @return Cell type
     */
    public static CellType random(CorticalLayer layer, Random random) {
        // Layer 4 has more inhibitory neurons (~30% instead of 20%)
        double inhibitoryProbability = (layer == CorticalLayer.LAYER_4) ? 0.3 : 0.2;

        return random.nextDouble() < inhibitoryProbability ? INHIBITORY : EXCITATORY;
    }
}
