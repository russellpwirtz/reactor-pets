package com.reactor.pets.query;

import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetStatusView {
  private String petId;
  private String name;
  private PetType type;
  private int hunger;
  private int happiness;
  private int health;
  private PetStage stage;
  private boolean isAlive;

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
