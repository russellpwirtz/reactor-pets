package com.reactor.pets.query;

import lombok.Value;

@Value
public class GetLeaderboardQuery {
  LeaderboardType type;

  public enum LeaderboardType {
    AGE,
    HAPPINESS,
    HEALTH
  }
}
