package com.reactor.pets.command;

import com.reactor.pets.aggregate.PetType;
import lombok.Value;

/**
 * Command to track pet creation in the PlayerProgression aggregate.
 * This is dispatched by the XPEarningSaga when a pet is created.
 */
@Value
public class TrackPetCreationCommand {
  String playerId;
  String petId;
  String petName;
  PetType petType;
}
