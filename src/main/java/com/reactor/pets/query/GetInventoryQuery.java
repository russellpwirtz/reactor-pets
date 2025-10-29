package com.reactor.pets.query;

import lombok.Value;

/**
 * Query to get the player's full inventory.
 */
@Value
public class GetInventoryQuery {
  String playerId;
}
