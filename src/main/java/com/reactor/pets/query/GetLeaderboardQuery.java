package com.reactor.pets.query;

public record GetLeaderboardQuery(LeaderboardType type) {
  public enum LeaderboardType {
    AGE,
    HAPPINESS,
    HEALTH
  }
}
