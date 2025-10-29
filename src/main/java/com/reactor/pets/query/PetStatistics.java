package com.reactor.pets.query;

import com.reactor.pets.aggregate.PetStage;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pet_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetStatistics {
  @Id
  private String id; // Single row with fixed ID "GLOBAL"

  private int totalPetsCreated;
  private int totalPetsDied;
  private String longestLivedPetId;
  private String longestLivedPetName;
  private int longestLivedPetAge;

  @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
  @CollectionTable(name = "stage_distribution", joinColumns = @JoinColumn(name = "statistics_id"))
  @MapKeyColumn(name = "stage")
  @MapKeyEnumerated(EnumType.STRING)
  @Column(name = "count")
  private Map<PetStage, Integer> stageDistribution = new HashMap<>();

  private Instant lastUpdated;

  public double getAverageLifespan() {
    if (totalPetsDied == 0) {
      return 0.0;
    }
    // This is a simplified average - in a real system we'd track total ages
    // For now, we'll estimate based on longest lived pet
    return longestLivedPetAge / 2.0; // Simplified approximation
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    sb.append("╔════════════════════════════════════════════════════════════╗\n");
    sb.append("║           GLOBAL PET STATISTICS DASHBOARD                 ║\n");
    sb.append("╚════════════════════════════════════════════════════════════╝\n");
    sb.append("\n");

    sb.append("Overview:\n");
    sb.append("---------\n");
    sb.append(String.format("  Total Pets Created: %d\n", totalPetsCreated));
    sb.append(String.format("  Total Pets Died: %d\n", totalPetsDied));
    sb.append(String.format("  Currently Alive: %d\n", totalPetsCreated - totalPetsDied));
    sb.append(
        String.format(
            "  Average Lifespan: %.1f ticks\n", getAverageLifespan())); // Simplified calculation
    sb.append("\n");

    if (longestLivedPetId != null) {
      sb.append("Hall of Fame:\n");
      sb.append("-------------\n");
      sb.append(String.format("  Longest Lived Pet: %s\n", longestLivedPetName));
      sb.append(String.format("  Age: %d ticks\n", longestLivedPetAge));
      sb.append(String.format("  ID: %s\n", longestLivedPetId));
      sb.append("\n");
    }

    if (!stageDistribution.isEmpty()) {
      sb.append("Evolution Stage Distribution:\n");
      sb.append("-----------------------------\n");
      for (Map.Entry<PetStage, Integer> entry : stageDistribution.entrySet()) {
        sb.append(String.format("  %-10s: %d pet(s)\n", entry.getKey(), entry.getValue()));
      }
      sb.append("\n");
    }

    sb.append(String.format("Last Updated: %s\n", lastUpdated));
    sb.append("\n");

    return sb.toString();
  }
}
