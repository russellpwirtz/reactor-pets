package com.reactor.pets.query;

import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
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
  private boolean isAlive;
  private Instant lastUpdated;

  @Override
  public String toString() {
    return String.format(
        """

                Pet Status:
                -----------
                ID: %s
                Name: %s
                Type: %s
                Stage: %s
                Status: %s

                Stats:
                  Hunger: %d/100
                  Happiness: %d/100
                  Health: %d/100
                """,
        petId, name, type, stage, isAlive ? "Alive" : "Dead", hunger, happiness, health);
  }
}
