package com.reactor.pets.projection;

import com.reactor.pets.domain.ItemDefinition;
import com.reactor.pets.domain.ShopCatalog;
import com.reactor.pets.query.GetPetCreationCostQuery;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.GetShopItemQuery;
import com.reactor.pets.query.GetShopItemsQuery;
import com.reactor.pets.query.PlayerProgressionView;
import com.reactor.pets.service.PetCreationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

/**
 * Projection for handling shop-related queries.
 * Shop catalog is static, so no event sourcing needed.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ShopProjection {

  private final QueryGateway queryGateway;
  private final PetCreationService petCreationService;

  /**
   * Handle query for all shop items, optionally filtered by type.
   */
  @QueryHandler
  public List<ItemDefinition> handle(GetShopItemsQuery query) {
    log.debug("Handling GetShopItemsQuery with filter: {}", query.getItemType());

    if (query.getItemType() == null) {
      return ShopCatalog.getAllItems();
    }

    return switch (query.getItemType()) {
      case EQUIPMENT -> ShopCatalog.getAllEquipment();
      case PERMANENT_UPGRADE -> ShopCatalog.getAllUpgrades();
    };
  }

  /**
   * Handle query for a specific shop item.
   */
  @QueryHandler
  public ItemDefinition handle(GetShopItemQuery query) {
    log.debug("Handling GetShopItemQuery for item: {}", query.getItemId());

    return ShopCatalog.getItem(query.getItemId())
        .orElse(null);
  }

  /**
   * Handle query for the cost of creating the next pet.
   */
  @QueryHandler
  public Long handle(GetPetCreationCostQuery query) {
    log.debug("Handling GetPetCreationCostQuery for player: {}", query.getPlayerId());

    try {
      // Query player progression to get pet count
      PlayerProgressionView progression = queryGateway
          .query(new GetPlayerProgressionQuery(query.getPlayerId()), PlayerProgressionView.class)
          .join();

      int totalPetsCreated = (progression != null) ? progression.getTotalPetsCreated() : 0;
      return petCreationService.calculatePetCost(totalPetsCreated);
    } catch (Exception e) {
      log.error("Error calculating pet creation cost", e);
      return 0L; // Default to FREE if error
    }
  }
}
