package com.reactor.pets.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_progression")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerProgressionView {
  @Id
  private String playerId;
  private long totalXP;
  private long lifetimeXPEarned;
  private int totalPetsCreated;
  private int prestigeLevel;
  private Instant lastUpdated;

  @Override
  public String toString() {
    return String.format(
        """

                Player Progression:
                ------------------
                Current XP: %d
                Lifetime XP: %d
                Total Pets Created: %d
                Prestige Level: %d
                """,
        totalXP,
        lifetimeXPEarned,
        totalPetsCreated,
        prestigeLevel);
  }
}
