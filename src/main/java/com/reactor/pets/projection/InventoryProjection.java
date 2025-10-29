package com.reactor.pets.projection;

import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.event.InventoryInitializedEvent;
import com.reactor.pets.event.ItemAddedToInventoryEvent;
import com.reactor.pets.event.ItemRemovedFromInventoryEvent;
import com.reactor.pets.query.GetInventoryItemQuery;
import com.reactor.pets.query.GetInventoryQuery;
import com.reactor.pets.query.InventoryRepository;
import com.reactor.pets.query.InventoryView;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ProcessingGroup("inventory")
@Slf4j
@RequiredArgsConstructor
public class InventoryProjection {

  private final InventoryRepository inventoryRepository;

  @EventHandler
  @Transactional
  public void on(InventoryInitializedEvent event) {
    log.debug("Processing InventoryInitializedEvent for playerId: {}", event.getPlayerId());

    InventoryView view = new InventoryView();
    view.setPlayerId(event.getPlayerId());
    view.setItems(new ArrayList<>(event.getStarterItems()));
    view.setLastUpdated(event.getTimestamp());

    inventoryRepository.save(view);
    log.info("Inventory initialized for player {} with {} items",
        event.getPlayerId(), event.getStarterItems().size());
  }

  @EventHandler
  @Transactional
  public void on(ItemAddedToInventoryEvent event) {
    log.debug("Processing ItemAddedToInventoryEvent for playerId: {}", event.getPlayerId());

    inventoryRepository
        .findById(event.getPlayerId())
        .ifPresent(
            view -> {
              view.getItems().add(event.getItem());
              view.setLastUpdated(event.getTimestamp());
              inventoryRepository.save(view);
              log.info("Item {} added to inventory for player {}",
                  event.getItem().getName(), event.getPlayerId());
            });
  }

  @EventHandler
  @Transactional
  public void on(ItemRemovedFromInventoryEvent event) {
    log.debug("Processing ItemRemovedFromInventoryEvent for playerId: {}", event.getPlayerId());

    inventoryRepository
        .findById(event.getPlayerId())
        .ifPresent(
            view -> {
              view.getItems().removeIf(item -> item.getItemId().equals(event.getItemId()));
              view.setLastUpdated(event.getTimestamp());
              inventoryRepository.save(view);
              log.info("Item {} removed from inventory for player {}",
                  event.getItemId(), event.getPlayerId());
            });
  }

  @QueryHandler
  public InventoryView handle(GetInventoryQuery query) {
    log.debug("Handling GetInventoryQuery for inventoryId: {}", query.getPlayerId());
    // Map playerId to inventoryId for single player mode
    String inventoryId = "PLAYER_INVENTORY";
    return inventoryRepository
        .findById(inventoryId)
        .orElse(null);
  }

  @QueryHandler
  public EquipmentItem handle(GetInventoryItemQuery query) {
    log.debug("Handling GetInventoryItemQuery for itemId: {}", query.getItemId());
    // Query uses inventoryId directly
    return inventoryRepository
        .findById(query.getPlayerId()) // This is actually the inventoryId
        .map(view -> view.getItems().stream()
            .filter(item -> item.getItemId().equals(query.getItemId()))
            .findFirst()
            .orElse(null))
        .orElse(null);
  }
}
