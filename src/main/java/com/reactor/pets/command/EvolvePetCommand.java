package com.reactor.pets.command;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to evolve a pet to a new stage.
 * This is typically triggered by the PetEvolutionSaga based on age and care quality.
 */
public record EvolvePetCommand(
        @TargetAggregateIdentifier String petId,
        PetStage newStage,
        EvolutionPath evolutionPath,
        String evolutionReason) { }
