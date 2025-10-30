package com.reactor.pets.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.reactor.pets.domain.ItemDefinition;
import com.reactor.pets.domain.ItemType;
import com.reactor.pets.query.GetPetCreationCostQuery;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.GetShopItemQuery;
import com.reactor.pets.query.GetShopItemsQuery;
import com.reactor.pets.query.PlayerProgressionView;
import com.reactor.pets.service.PetCreationService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ShopProjection.
 */
@DisplayName("Shop Projection")
@ExtendWith(MockitoExtension.class)
class ShopProjectionTest {

  @Mock
  private QueryGateway queryGateway;

  @Mock
  private PetCreationService petCreationService;

  private ShopProjection projection;

  @BeforeEach
  void setUp() {
    projection = new ShopProjection(queryGateway, petCreationService);
  }

  @Test
  @DisplayName("should return all shop items when no filter is specified")
  void shouldReturnAllItemsWithNoFilter() {
    // Given
    GetShopItemsQuery query = new GetShopItemsQuery(null);

    // When
    List<ItemDefinition> items = projection.handle(query);

    // Then
    assertThat(items).isNotEmpty();
    assertThat(items).anyMatch(item -> item.getItemType() == ItemType.EQUIPMENT);
    assertThat(items).anyMatch(item -> item.getItemType() == ItemType.PERMANENT_UPGRADE);
  }

  @Test
  @DisplayName("should return only equipment when EQUIPMENT filter is specified")
  void shouldReturnOnlyEquipment() {
    // Given
    GetShopItemsQuery query = new GetShopItemsQuery(ItemType.EQUIPMENT);

    // When
    List<ItemDefinition> items = projection.handle(query);

    // Then
    assertThat(items).isNotEmpty();
    assertThat(items).allMatch(item -> item.getItemType() == ItemType.EQUIPMENT);
  }

  @Test
  @DisplayName("should return only upgrades when PERMANENT_UPGRADE filter is specified")
  void shouldReturnOnlyUpgrades() {
    // Given
    GetShopItemsQuery query = new GetShopItemsQuery(ItemType.PERMANENT_UPGRADE);

    // When
    List<ItemDefinition> items = projection.handle(query);

    // Then
    assertThat(items).isNotEmpty();
    assertThat(items).allMatch(item -> item.getItemType() == ItemType.PERMANENT_UPGRADE);
  }

  @Test
  @DisplayName("should return specific item when queried by ID")
  void shouldReturnSpecificItem() {
    // Given - using a known item ID from ShopCatalog
    GetShopItemQuery query = new GetShopItemQuery("NUTRIENT_BOWL");

    // When
    ItemDefinition item = projection.handle(query);

    // Then
    assertThat(item).isNotNull();
    assertThat(item.getItemId()).isEqualTo("NUTRIENT_BOWL");
  }

  @Test
  @DisplayName("should return null for non-existent item")
  void shouldReturnNullForNonExistentItem() {
    // Given
    GetShopItemQuery query = new GetShopItemQuery("NON_EXISTENT_ITEM");

    // When
    ItemDefinition item = projection.handle(query);

    // Then
    assertThat(item).isNull();
  }

  @Test
  @DisplayName("should calculate pet creation cost based on player progression")
  void shouldCalculatePetCreationCost() {
    // Given
    String playerId = "PLAYER_1";
    PlayerProgressionView progression = new PlayerProgressionView();
    progression.setPlayerId(playerId);
    progression.setTotalPetsCreated(3);

    when(queryGateway.query(any(GetPlayerProgressionQuery.class), any(Class.class)))
        .thenReturn(CompletableFuture.completedFuture(progression));
    when(petCreationService.calculatePetCost(3)).thenReturn(200L);

    GetPetCreationCostQuery query = new GetPetCreationCostQuery(playerId);

    // When
    Long cost = projection.handle(query);

    // Then
    assertThat(cost).isEqualTo(200L);
  }

  @Test
  @DisplayName("should return default cost when player progression is null")
  void shouldReturnDefaultCostWhenProgressionIsNull() {
    // Given
    String playerId = "PLAYER_1";

    when(queryGateway.query(any(GetPlayerProgressionQuery.class), any(Class.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
    when(petCreationService.calculatePetCost(0)).thenReturn(0L);

    GetPetCreationCostQuery query = new GetPetCreationCostQuery(playerId);

    // When
    Long cost = projection.handle(query);

    // Then
    assertThat(cost).isEqualTo(0L);
  }

  @Test
  @DisplayName("should return 0 when error occurs calculating cost")
  void shouldReturnZeroOnError() {
    // Given
    String playerId = "PLAYER_1";

    when(queryGateway.query(any(GetPlayerProgressionQuery.class), any(Class.class)))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test error")));

    GetPetCreationCostQuery query = new GetPetCreationCostQuery(playerId);

    // When
    Long cost = projection.handle(query);

    // Then
    assertThat(cost).isEqualTo(0L);
  }
}
