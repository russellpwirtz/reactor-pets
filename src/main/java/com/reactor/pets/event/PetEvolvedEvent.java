package com.reactor.pets.event;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import java.time.Instant;
import lombok.Value;

/**
 * Event emitted when a pet evolves to a new stage.
 * This affects the pet's appearance, stat caps, and degradation rates.
 */
@Value
public class PetEvolvedEvent {
  String petId;
  PetStage oldStage;
  PetStage newStage;
  EvolutionPath evolutionPath;
  String evolutionReason;
  Instant timestamp;
}
