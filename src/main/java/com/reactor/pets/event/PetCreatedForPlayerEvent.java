package com.reactor.pets.event;

import com.reactor.pets.aggregate.PetType;
import java.time.Instant;
import lombok.Value;

@Value
public class PetCreatedForPlayerEvent {
  String playerId;
  String petId;
  String petName;
  PetType petType;
  int totalPetsCreated; // The new count after this pet
  Instant timestamp;
}
