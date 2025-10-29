package com.reactor.pets.query;

import lombok.Value;

/**
 * Query to get a specific item from the player's inventory.
 */
@Value
public class GetInventoryItemQuery {
  String playerId;
  String itemId;
}
