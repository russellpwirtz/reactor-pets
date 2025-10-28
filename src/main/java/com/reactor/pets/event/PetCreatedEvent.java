package com.reactor.pets.event;

import com.reactor.pets.aggregate.PetType;
import java.time.Instant;
import lombok.Value;

@Value
public class PetCreatedEvent {
  String petId;
  String name;
  PetType type;
  Instant timestamp;
}
