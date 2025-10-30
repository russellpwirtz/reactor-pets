package com.reactor.pets.brain.model;

/**
 * Cortical layer structure representing different depths in a neural column.
 * Based on the laminar organization of mammalian neocortex.
 */
public enum CorticalLayer {
    /**
     * Layers 2/3: Superficial layers
     * - Local processing and inter-columnar communication
     * - Dense recurrent connections
     * - Integration of bottom-up and top-down information
     */
    LAYER_2_3(0, "L2/3", 0.8),

    /**
     * Layer 4: Input layer
     * - Primary recipient of sensory/thalamic input
     * - Fast-spiking interneurons (higher firing rate)
     * - Distributes input to other layers
     */
    LAYER_4(1, "L4", 1.2),

    /**
     * Layer 5: Deep pyramidal layer
     * - Large pyramidal neurons with bursting capability
     * - Long-range output projections (motor, subcortical)
     * - Intrinsic burst mode for amplification
     */
    LAYER_5(2, "L5", 1.0),

    /**
     * Layer 6: Deepest layer
     * - Corticothalamic feedback
     * - Modulatory influence on layer 4
     * - Lower baseline firing rate
     */
    LAYER_6(3, "L6", 0.6);

    private final int depth;
    private final String displayName;
    private final double firingMultiplier; // Affects how easily neurons in this layer fire

    CorticalLayer(int depth, String displayName, double firingMultiplier) {
        this.depth = depth;
        this.displayName = displayName;
        this.firingMultiplier = firingMultiplier;
    }

    public int getDepth() {
        return depth;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getFiringMultiplier() {
        return firingMultiplier;
    }

    /**
     * Assign layer based on Y-position in grid.
     * Creates vertical columns through layers.
     *
     * @param y Y-coordinate in grid
     * @param gridHeight Total height of grid
     * @return Cortical layer for this position
     */
    public static CorticalLayer fromYPosition(int y, int gridHeight) {
        // Divide grid into 4 equal horizontal bands (layers)
        // Top = superficial (L2/3), Bottom = deep (L6)
        double ratio = (double) y / gridHeight;

        if (ratio < 0.25) {
            return LAYER_2_3;
        } else if (ratio < 0.5) {
            return LAYER_4;
        } else if (ratio < 0.75) {
            return LAYER_5;
        } else {
            return LAYER_6;
        }
    }
}
