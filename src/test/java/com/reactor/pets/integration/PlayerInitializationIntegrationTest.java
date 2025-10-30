package com.reactor.pets.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.reactor.pets.query.GetInventoryQuery;
import com.reactor.pets.query.GetPlayerProgressionQuery;
import com.reactor.pets.query.InventoryView;
import com.reactor.pets.query.PlayerProgressionView;
import java.util.concurrent.TimeUnit;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for player and inventory initialization.
 * Verifies that the PlayerInitializationService correctly initializes
 * both the player progression and inventory on application startup.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Player Initialization Integration Test")
class PlayerInitializationIntegrationTest {

        @Autowired
        private QueryGateway queryGateway;

        private static final String PLAYER_ID = "PLAYER_1";
        private static final String INVENTORY_ID = "PLAYER_1_INVENTORY";

        @Test
        @DisplayName("should initialize player with starting XP")
        void shouldInitializePlayerWithStartingXP() throws Exception {
                // Allow time for ApplicationReadyEvent to trigger
                TimeUnit.MILLISECONDS.sleep(100);

                // When: Query for player progression
                PlayerProgressionView player = queryGateway
                                .query(new GetPlayerProgressionQuery(PLAYER_ID),
                                                ResponseTypes.instanceOf(PlayerProgressionView.class))
                                .join();

                // Then: Player should exist with starting XP
                assertThat(player).isNotNull();
                assertThat(player.getPlayerId()).isEqualTo(PLAYER_ID);
                assertThat(player.getTotalXP()).isGreaterThanOrEqualTo(100); // Starting XP = 100
                assertThat(player.getPrestigeLevel()).isEqualTo(0);
        }

        @Test
        @DisplayName("should initialize inventory with starter equipment")
        void shouldInitializeInventoryWithStarterEquipment() throws Exception {
                // Allow time for ApplicationReadyEvent to trigger
                TimeUnit.MILLISECONDS.sleep(100);

                // When: Query for inventory
                InventoryView inventory = queryGateway
                                .query(new GetInventoryQuery(INVENTORY_ID),
                                                ResponseTypes.instanceOf(InventoryView.class))
                                .join();

                // Then: Inventory should exist with 3 starter items
                assertThat(inventory).isNotNull();
                assertThat(inventory.getPlayerId()).isEqualTo(INVENTORY_ID);
                assertThat(inventory.getItems()).isNotNull();
                assertThat(inventory.getItems()).hasSize(3);

                // Verify starter items are present
                assertThat(inventory.getItems())
                                .extracting("name")
                                .containsExactlyInAnyOrder("Basic Bowl", "Simple Toy", "Comfort Blanket");
        }

        @Test
        @DisplayName("should have distinct IDs for player and inventory")
        void shouldHaveDistinctIDsForPlayerAndInventory() throws Exception {
                // Allow time for ApplicationReadyEvent to trigger
                TimeUnit.MILLISECONDS.sleep(100);

                // When: Query both aggregates
                PlayerProgressionView player = queryGateway
                                .query(new GetPlayerProgressionQuery(PLAYER_ID),
                                                ResponseTypes.instanceOf(PlayerProgressionView.class))
                                .join();

                InventoryView inventory = queryGateway
                                .query(new GetInventoryQuery(INVENTORY_ID),
                                                ResponseTypes.instanceOf(InventoryView.class))
                                .join();

                // Then: IDs should be different
                assertThat(player.getPlayerId()).isNotEqualTo(inventory.getPlayerId());
                assertThat(player.getPlayerId()).isEqualTo(PLAYER_ID);
                assertThat(inventory.getPlayerId()).isEqualTo(INVENTORY_ID);
        }
}
