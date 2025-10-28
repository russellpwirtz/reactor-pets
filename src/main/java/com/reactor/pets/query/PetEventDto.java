package com.reactor.pets.query;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetEventDto {
  private String eventType;
  private Instant timestamp;
  private String details;
}
