package com.reactor.pets.query;

import lombok.Value;

/**
 * Query to get the XP cost for creating the next pet.
 */
@Value
public class GetPetCreationCostQuery {
  String playerId; // For future multi-player support
}
