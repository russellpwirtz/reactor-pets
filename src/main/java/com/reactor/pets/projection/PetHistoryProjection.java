package com.reactor.pets.projection;

import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import com.reactor.pets.query.GetPetHistoryQuery;
import com.reactor.pets.query.PetEventDto;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PetHistoryProjection {

  private final EventStore eventStore;

  @QueryHandler
  public List<PetEventDto> handle(GetPetHistoryQuery query) {
    log.debug("Handling GetPetHistoryQuery for petId: {}", query.getPetId());

    List<PetEventDto> events = new ArrayList<>();
    int limit = Math.min(query.getLimit(), 50); // Cap at 50 events

    eventStore
        .readEvents(query.getPetId())
        .asStream()
        .map(this::convertToDto)
        .forEach(events::add);

    // Return the last N events (most recent)
    if (events.size() > limit) {
      return events.stream().skip(events.size() - limit).collect(Collectors.toList());
    }

    return events;
  }

  private PetEventDto convertToDto(DomainEventMessage<?> message) {
    Object payload = message.getPayload();
    String eventType = payload.getClass().getSimpleName();
    String details = formatEventDetails(payload);

    return new PetEventDto(eventType, message.getTimestamp(), details);
  }

  private String formatEventDetails(Object event) {
    return switch (event) {
      case PetCreatedEvent e -> String.format("Pet '%s' (%s) was created", e.getName(), e.getType());
      case PetFedEvent e -> String.format("Pet was fed, hunger reduced by %d", e.getHungerReduction());
      case PetPlayedWithEvent e -> String.format(
          "Played with pet, happiness increased by %d, hunger increased by %d",
          e.getHappinessIncrease(), e.getHungerIncrease());
      case PetCleanedEvent e -> String.format("Pet was cleaned, health increased by %d", e.getHealthIncrease());
      default -> "Unknown event: " + event.getClass().getSimpleName();
    };
  }
}
