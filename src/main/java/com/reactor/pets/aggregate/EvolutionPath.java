package com.reactor.pets.aggregate;

/**
 * Represents the quality of care that has been given to a pet.
 * This affects stat degradation rates and determines which evolution variant the pet becomes.
 */
public enum EvolutionPath {
    /**
     * Pet has received good care with consistently high stats.
     * Results in better base stats and slower degradation rates.
     */
    HEALTHY,

    /**
     * Pet has received poor care with frequently low stats.
     * Results in lower base stats and faster degradation rates.
     */
    NEGLECTED,

    /**
     * Initial state - not yet determined.
     * Path is set when pet first evolves from EGG to BABY.
     */
    UNDETERMINED
}
