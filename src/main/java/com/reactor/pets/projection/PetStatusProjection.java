package com.reactor.pets.projection;

import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.query.GetPetStatusQuery;
import com.reactor.pets.query.PetStatusView;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("pet-status")
@Slf4j
public class PetStatusProjection {

  private final Map<String, PetStatusView> petStatusStore = new ConcurrentHashMap<>();

  @EventHandler
  public void on(PetCreatedEvent event) {
    log.debug("Processing PetCreatedEvent for petId: {}", event.getPetId());

    PetStatusView view = new PetStatusView();
    view.setPetId(event.getPetId());
    view.setName(event.getName());
    view.setType(event.getType());
    view.setHunger(30);
    view.setHappiness(70);
    view.setHealth(100);
    view.setStage(com.reactor.pets.aggregate.PetStage.EGG);
    view.setAlive(true);

    petStatusStore.put(event.getPetId(), view);
    log.info("Pet created: {} ({})", event.getName(), event.getType());
  }

  @EventHandler
  public void on(PetFedEvent event) {
    log.debug("Processing PetFedEvent for petId: {}", event.getPetId());

    PetStatusView view = petStatusStore.get(event.getPetId());
    if (view != null) {
      int newHunger = Math.max(0, view.getHunger() - event.getHungerReduction());
      view.setHunger(newHunger);
      log.info(
          "Pet {} fed. Hunger reduced by {} to {}",
          view.getName(),
          event.getHungerReduction(),
          newHunger);
    }
  }

  @QueryHandler
  public PetStatusView handle(GetPetStatusQuery query) {
    log.debug("Handling GetPetStatusQuery for petId: {}", query.getPetId());

    PetStatusView view = petStatusStore.get(query.getPetId());
    if (view == null) {
      throw new IllegalArgumentException("Pet not found: " + query.getPetId());
    }
    return view;
  }
}
