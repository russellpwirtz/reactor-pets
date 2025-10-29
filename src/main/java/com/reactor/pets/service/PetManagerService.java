package com.reactor.pets.service;

import com.reactor.pets.query.GetAlivePetsQuery;
import com.reactor.pets.query.GetLeaderboardQuery;
import com.reactor.pets.query.GetStatisticsQuery;
import com.reactor.pets.query.PetStatistics;
import com.reactor.pets.query.PetStatusView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

/**
 * Service for managing multiple pets and accessing global statistics.
 * Provides high-level operations for pet management and statistics queries.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetManagerService {

  private final QueryGateway queryGateway;

  /**
   * Get all currently alive pets.
   *
   * @return List of alive pets
   */
  public List<PetStatusView> getAlivePets() {
    log.debug("Getting all alive pets");
    return queryGateway
        .query(new GetAlivePetsQuery(), ResponseTypes.multipleInstancesOf(PetStatusView.class))
        .join();
  }

  /**
   * Get global statistics across all pets.
   *
   * @return Global pet statistics
   */
  public PetStatistics getGlobalStatistics() {
    log.debug("Getting global statistics");
    return queryGateway.query(new GetStatisticsQuery(), PetStatistics.class).join();
  }

  /**
   * Get leaderboard of pets sorted by the specified metric.
   *
   * @param type The type of leaderboard (AGE, HAPPINESS, HEALTH)
   * @param aliveOnly Whether to filter for only alive pets
   * @return Top 10 pets sorted by the specified metric
   */
  public List<PetStatusView> getLeaderboard(GetLeaderboardQuery.LeaderboardType type, boolean aliveOnly) {
    log.debug("Getting leaderboard for type: {}, aliveOnly: {}", type, aliveOnly);
    return queryGateway
        .query(
            new GetLeaderboardQuery(type, aliveOnly), ResponseTypes.multipleInstancesOf(PetStatusView.class))
        .join();
  }

  /**
   * Get a formatted dashboard string with statistics and all alive pets.
   *
   * @return Formatted dashboard string
   */
  public String getDashboard() {
    log.debug("Generating dashboard");

    PetStatistics stats = getGlobalStatistics();
    List<PetStatusView> alivePets = getAlivePets();

    StringBuilder sb = new StringBuilder();
    sb.append(stats.toString());

    if (!alivePets.isEmpty()) {
      sb.append("╔════════════════════════════════════════════════════════════╗\n");
      sb.append("║                   ALL ALIVE PETS                          ║\n");
      sb.append("╚════════════════════════════════════════════════════════════╝\n");
      sb.append("\n");

      sb.append(
          String.format(
              "%-36s | %-15s | %-8s | %-6s | Stats%n", "ID", "Name", "Type", "Stage"));
      sb.append("-".repeat(95)).append("\n");

      for (PetStatusView pet : alivePets) {
        String healthIndicator = getHealthIndicator(pet.getHealth());
        String hungerIndicator = getHungerIndicator(pet.getHunger());
        String happinessIndicator = getHappinessIndicator(pet.getHappiness());

        sb.append(
            String.format(
                "%-36s | %-15s | %-8s | %-6s | H:%3d%s F:%3d%s Hp:%3d%s Age:%3d%n",
                pet.getPetId(),
                pet.getName(),
                pet.getType(),
                pet.getStage(),
                pet.getHealth(),
                healthIndicator,
                pet.getHunger(),
                hungerIndicator,
                pet.getHappiness(),
                happinessIndicator,
                pet.getAge()));
      }
      sb.append("\n");
    } else {
      sb.append("No alive pets. Create one with 'create <name> <type>'\n\n");
    }

    return sb.toString();
  }

  /**
   * Get a formatted leaderboard string for the specified type.
   *
   * @param type The type of leaderboard (AGE, HAPPINESS, HEALTH)
   * @return Formatted leaderboard string
   */
  public String getLeaderboardDisplay(GetLeaderboardQuery.LeaderboardType type) {
    log.debug("Generating leaderboard display for type: {}", type);

    List<PetStatusView> topPets = getLeaderboard(type, true);

    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    sb.append("╔════════════════════════════════════════════════════════════╗\n");
    sb.append(String.format("║          LEADERBOARD - TOP %s                      ║%n", type));
    sb.append("╚════════════════════════════════════════════════════════════╝\n");
    sb.append("\n");

    if (topPets.isEmpty()) {
      sb.append("No pets found.\n\n");
      return sb.toString();
    }

    String metricName =
        switch (type) {
          case AGE -> "Age";
          case HAPPINESS -> "Happiness";
          case HEALTH -> "Health";
        };

    sb.append(
        String.format(
            "%-4s | %-20s | %-8s | %-6s | %s%n", "Rank", "Name", "Type", "Stage", metricName));
    sb.append("-".repeat(60)).append("\n");

    int rank = 1;
    for (PetStatusView pet : topPets) {
      int metricValue =
          switch (type) {
            case AGE -> pet.getAge();
            case HAPPINESS -> pet.getHappiness();
            case HEALTH -> pet.getHealth();
          };

      String trophy = rank == 1 ? "🏆" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "  ";

      sb.append(
          String.format(
              "%s%-2d | %-20s | %-8s | %-6s | %d%n",
              trophy, rank, pet.getName(), pet.getType(), pet.getStage(), metricValue));
      rank++;
    }
    sb.append("\n");

    return sb.toString();
  }

  private String getHealthIndicator(int health) {
    if (health < 30) {
      return "🔴";
    } else if (health < 50) {
      return "🟡";
    }
    return "  ";
  }

  private String getHungerIndicator(int hunger) {
    if (hunger > 70) {
      return "🔴";
    } else if (hunger > 50) {
      return "🟡";
    }
    return "  ";
  }

  private String getHappinessIndicator(int happiness) {
    if (happiness < 20) {
      return "🔴";
    } else if (happiness < 40) {
      return "🟡";
    }
    return "  ";
  }
}
