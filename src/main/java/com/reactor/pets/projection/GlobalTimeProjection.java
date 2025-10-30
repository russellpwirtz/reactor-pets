package com.reactor.pets.projection;

import com.reactor.pets.event.GlobalTimeAdvancedEvent;
import com.reactor.pets.event.GlobalTimeCreatedEvent;
import com.reactor.pets.query.GetGlobalTimeQuery;
import com.reactor.pets.query.GlobalTimeRepository;
import com.reactor.pets.query.GlobalTimeView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ProcessingGroup("global-time")
@Slf4j
@RequiredArgsConstructor
public class GlobalTimeProjection {

  private final GlobalTimeRepository globalTimeRepository;

  @EventHandler
  @Transactional
  public void on(GlobalTimeCreatedEvent event) {
    log.debug("Processing GlobalTimeCreatedEvent for timeId: {}", event.getTimeId());

    GlobalTimeView view = new GlobalTimeView();
    view.setTimeId(event.getTimeId());
    view.setCurrentGlobalTick(0L);
    view.setLastUpdated(event.getTimestamp());

    globalTimeRepository.save(view);
    log.info("GlobalTime created with ID: {}", event.getTimeId());
  }

  @EventHandler
  @Transactional
  public void on(GlobalTimeAdvancedEvent event) {
    log.debug("Processing GlobalTimeAdvancedEvent: tick {}", event.getNewGlobalTick());

    globalTimeRepository
        .findById(event.getTimeId())
        .ifPresent(
            view -> {
              view.setCurrentGlobalTick(event.getNewGlobalTick());
              view.setLastUpdated(event.getTimestamp());
              globalTimeRepository.save(view);
              log.debug("GlobalTime advanced to tick {}", event.getNewGlobalTick());
            });
  }

  @QueryHandler
  public GlobalTimeView handle(GetGlobalTimeQuery query) {
    // Return the singleton global time view
    // If it doesn't exist yet, it will be null
    return globalTimeRepository.findAll().stream().findFirst().orElse(null);
  }
}
