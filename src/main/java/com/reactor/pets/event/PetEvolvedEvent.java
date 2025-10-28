package com.reactor.pets.event;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import java.time.Instant;

/**
 * Event emitted when a pet evolves to a new stage.
 * This affects the pet's appearance, stat caps, and degradation rates.
 */
public record PetEvolvedEvent(
        String petId,
        PetStage oldStage,
        PetStage newStage,
        EvolutionPath evolutionPath,
        String evolutionReason,
        Instant timestamp) { }
