package com.reactor.pets.query;

import com.reactor.pets.domain.UpgradeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "player_upgrades", joinColumns = @JoinColumn(name = "player_id"))
  @Column(name = "upgrade_type")
  @Enumerated(EnumType.STRING)
  private Set<UpgradeType> permanentUpgrades = new HashSet<>();

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
                Permanent Upgrades: %s
                """,
        totalXP,
        lifetimeXPEarned,
        totalPetsCreated,
        prestigeLevel,
        permanentUpgrades != null ? permanentUpgrades.size() : 0);
  }
}
