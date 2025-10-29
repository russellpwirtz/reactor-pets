package com.reactor.pets.projection;

import com.reactor.pets.event.PetCreatedForPlayerEvent;
import com.reactor.pets.event.PlayerInitializedEvent;
import com.reactor.pets.event.UpgradePurchasedEvent;
import com.reactor.pets.event.XPEarnedEvent;
import com.reactor.pets.event.XPSpentEvent;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.PlayerProgressionRepository;
import com.reactor.pets.query.PlayerProgressionView;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ProcessingGroup("player-progression")
@Slf4j
@RequiredArgsConstructor
public class PlayerProgressionProjection {

  private final PlayerProgressionRepository playerProgressionRepository;

  @EventHandler
  @Transactional
  public void on(PlayerInitializedEvent event) {
    log.debug("Processing PlayerInitializedEvent for playerId: {}", event.getPlayerId());

    PlayerProgressionView view = new PlayerProgressionView();
    view.setPlayerId(event.getPlayerId());
    view.setTotalXP(event.getStartingXP());
    view.setLifetimeXPEarned(event.getStartingXP());
    view.setTotalPetsCreated(0);
    view.setPrestigeLevel(0);
    view.setPermanentUpgrades(new HashSet<>());
    view.setLastUpdated(event.getTimestamp());

    playerProgressionRepository.save(view);
    log.info("Player initialized with {} starting XP", event.getStartingXP());
  }

  @EventHandler
  @Transactional
  public void on(XPEarnedEvent event) {
    log.debug("Processing XPEarnedEvent for playerId: {}", event.getPlayerId());

    playerProgressionRepository
        .findById(event.getPlayerId())
        .ifPresent(
            view -> {
              view.setTotalXP(event.getNewTotalXP());
              view.setLifetimeXPEarned(event.getNewLifetimeXP());
              view.setLastUpdated(event.getTimestamp());
              playerProgressionRepository.save(view);
              log.info(
                  "Player earned {} XP from '{}'. New total: {}, Lifetime: {}",
                  event.getXpAmount(),
                  event.getSource(),
                  event.getNewTotalXP(),
                  event.getNewLifetimeXP());
            });
  }

  @EventHandler
  @Transactional
  public void on(XPSpentEvent event) {
    log.debug("Processing XPSpentEvent for playerId: {}", event.getPlayerId());

    playerProgressionRepository
        .findById(event.getPlayerId())
        .ifPresent(
            view -> {
              view.setTotalXP(event.getNewTotalXP());
              view.setLastUpdated(event.getTimestamp());
              playerProgressionRepository.save(view);
              log.info(
                  "Player spent {} XP on '{}'. New total: {}",
                  event.getXpAmount(),
                  event.getPurpose(),
                  event.getNewTotalXP());
            });
  }

  @EventHandler
  @Transactional
  public void on(PetCreatedForPlayerEvent event) {
    log.debug("Processing PetCreatedForPlayerEvent for playerId: {}", event.getPlayerId());

    playerProgressionRepository
        .findById(event.getPlayerId())
        .ifPresent(
            view -> {
              view.setTotalPetsCreated(event.getTotalPetsCreated());
              view.setLastUpdated(event.getTimestamp());
              playerProgressionRepository.save(view);
              log.info(
                  "Player pet count updated to {}. Pet: {} ({})",
                  event.getTotalPetsCreated(),
                  event.getPetName(),
                  event.getPetType());
            });
  }

  @EventHandler
  @Transactional
  public void on(UpgradePurchasedEvent event) {
    log.debug("Processing UpgradePurchasedEvent for playerId: {}", event.getPlayerId());

    playerProgressionRepository
        .findById(event.getPlayerId())
        .ifPresent(
            view -> {
              if (view.getPermanentUpgrades() == null) {
                view.setPermanentUpgrades(new HashSet<>());
              }
              view.getPermanentUpgrades().add(event.getUpgradeType());
              view.setLastUpdated(event.getTimestamp());
              playerProgressionRepository.save(view);
              log.info(
                  "Player purchased upgrade: {}. Total upgrades: {}",
                  event.getUpgradeType(),
                  view.getPermanentUpgrades().size());
            });
  }

  @QueryHandler
  public PlayerProgressionView handle(GetPlayerProgressionQuery query) {
    log.debug("Handling GetPlayerProgressionQuery for playerId: {}", query.getPlayerId());
    return playerProgressionRepository
        .findById(query.getPlayerId())
        .orElse(null); // Return null if player not initialized yet
  }
}
