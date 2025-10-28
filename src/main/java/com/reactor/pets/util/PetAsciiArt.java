package com.reactor.pets.util;

import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;

/** Utility class for generating ASCII art representations of pets. */
public final class PetAsciiArt {

  private PetAsciiArt() {
    // Utility class
  }

  public static String getArt(PetType type, PetStage stage) {
    return switch (stage) {
      case EGG -> getEggArt();
      case BABY -> getBabyArt(type);
      case TEEN -> getTeenArt(type);
      case ADULT -> getAdultArt(type);
    };
  }

  private static String getEggArt() {
    return """
                    ___
                  /     \\
                 |  o o  |
                  \\  ^  /
                   -----
            """;
  }

  private static String getBabyArt(PetType type) {
    return switch (type) {
      case DOG -> """
                  /\\_/\\
                 ( o.o )
                  > ^ <
                 /|   |\\
            """;
      case CAT -> """
                 /\\_/\\
                ( ^.^ )
                 > < <
                /     \\
            """;
      case DRAGON -> """
                  />_<\\
                 ( o.o )
                  )~w~(
                 /|   |\\
            """;
    };
  }

  private static String getTeenArt(PetType type) {
    return switch (type) {
      case DOG -> """
                 /\\_/\\
                ( o.o )
                 > ^ <
                /|   |\\
                 |   |
            """;
      case CAT -> """
                /\\_/\\
               ( -.-)
                > v <
               /     \\
               |  |  |
            """;
      case DRAGON -> """
                />_<\\
               ( O.O )
                )~~~(
               /|   |\\
              ~ |   | ~
            """;
    };
  }

  private static String getAdultArt(PetType type) {
    return switch (type) {
      case DOG -> """
                  /\\_/\\
                 ( O.O )
                  > ^ <
                 /|   |\\
                  |   |
                 /     \\
            """;
      case CAT -> """
                 /\\_/\\
                ( O.O )
                 > W <
                /     \\
                |  |  |
               /   |   \\
            """;
      case DRAGON -> """
                 />___<\\
                ( O.O )
                 )~~~(
                /|   |\\
               ~ |   | ~
               \\ |   | /
                 \\___/
            """;
    };
  }
}
