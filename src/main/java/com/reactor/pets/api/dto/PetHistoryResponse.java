package com.reactor.pets.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event history for a pet")
public class PetHistoryResponse {

  @Schema(description = "Pet ID", example = "123e4567-e89b-12d3-a456-426614174000")
  private String petId;

  @Schema(description = "List of events")
  private List<EventEntry> events;

  @Schema(description = "Total number of events", example = "25")
  private int totalEvents;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Individual event entry")
  public static class EventEntry {

    @Schema(description = "Event type", example = "PetFedEvent")
    private String eventType;

    @Schema(description = "Event timestamp")
    private Instant timestamp;

    @Schema(description = "Event payload as JSON string")
    private Object payload;
  }
}
