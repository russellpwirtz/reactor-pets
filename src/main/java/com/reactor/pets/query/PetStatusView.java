package com.reactor.pets.query;

import com.reactor.pets.aggregate.EvolutionPath;
import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import com.reactor.pets.util.PetAsciiArt;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pet_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetStatusView {
  @Id
  private String petId;
  private String name;
  @Enumerated(EnumType.STRING)
  private PetType type;
  private int hunger;
  private int happiness;
  private int health;
  @Enumerated(EnumType.STRING)
  private PetStage stage;
  @Enumerated(EnumType.STRING)
  private EvolutionPath evolutionPath;
  private boolean isAlive;
  private int age;
  private int totalTicks;
  private double xpMultiplier;
  private Instant lastUpdated;

  @Override
  public String toString() {
    String hungerIndicator = hunger > 70 ? " 🔴" : hunger > 50 ? " 🟡" : "";
    String happinessIndicator = happiness < 20 ? " 🔴" : happiness < 40 ? " 🟡" : "";
    String healthIndicator = health < 30 ? " 🔴" : health < 50 ? " 🟡" : "";

    String evolutionPathDisplay =
        evolutionPath == EvolutionPath.UNDETERMINED
            ? "Not yet determined"
            : evolutionPath.toString();
    String asciiArt = PetAsciiArt.getArt(type, stage);

    return String.format(
        """

                %s
                Pet Status:
                -----------
                ID: %s
                Name: %s
                Type: %s
                Stage: %s
                Evolution Path: %s
                Status: %s
                Age: %d
                XP Multiplier: %.2fx

                Stats:
                  Hunger: %d/100%s
                  Happiness: %d/100%s
                  Health: %d/100%s
                """,
        asciiArt,
        petId,
        name,
        type,
        stage,
        evolutionPathDisplay,
        isAlive ? "Alive" : "Dead",
        age,
        xpMultiplier,
        hunger,
        hungerIndicator,
        happiness,
        happinessIndicator,
        health,
        healthIndicator);
  }
}
