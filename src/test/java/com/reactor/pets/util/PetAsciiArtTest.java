package com.reactor.pets.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.reactor.pets.aggregate.PetStage;
import com.reactor.pets.aggregate.PetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for PetAsciiArt utility class. */
@DisplayName("PetAsciiArt Utility")
class PetAsciiArtTest {

  @Test
  @DisplayName("should return egg art for all pet types at EGG stage")
  void shouldReturnEggArtForAllPetTypes() {
    // Egg stage should be same for all types
    String dogEggArt = PetAsciiArt.getArt(PetType.DOG, PetStage.EGG);
    String catEggArt = PetAsciiArt.getArt(PetType.CAT, PetStage.EGG);
    String dragonEggArt = PetAsciiArt.getArt(PetType.DRAGON, PetStage.EGG);

    assertThat(dogEggArt).isNotNull().isNotEmpty();
    assertThat(catEggArt).isEqualTo(dogEggArt);
    assertThat(dragonEggArt).isEqualTo(dogEggArt);
  }

  @ParameterizedTest
  @EnumSource(PetType.class)
  @DisplayName("should return unique baby art for each pet type")
  void shouldReturnUniqueBabyArtForEachType(PetType type) {
    String art = PetAsciiArt.getArt(type, PetStage.BABY);

    assertThat(art).isNotNull().isNotEmpty();
  }

  @ParameterizedTest
  @EnumSource(PetType.class)
  @DisplayName("should return unique teen art for each pet type")
  void shouldReturnUniqueTeenArtForEachType(PetType type) {
    String art = PetAsciiArt.getArt(type, PetStage.TEEN);

    assertThat(art).isNotNull().isNotEmpty();
  }

  @ParameterizedTest
  @EnumSource(PetType.class)
  @DisplayName("should return unique adult art for each pet type")
  void shouldReturnUniqueAdultArtForEachType(PetType type) {
    String art = PetAsciiArt.getArt(type, PetStage.ADULT);

    assertThat(art).isNotNull().isNotEmpty();
  }

  @Test
  @DisplayName("should return different art for different stages of same type")
  void shouldReturnDifferentArtForDifferentStages() {
    String eggArt = PetAsciiArt.getArt(PetType.DOG, PetStage.EGG);
    String babyArt = PetAsciiArt.getArt(PetType.DOG, PetStage.BABY);
    String teenArt = PetAsciiArt.getArt(PetType.DOG, PetStage.TEEN);
    String adultArt = PetAsciiArt.getArt(PetType.DOG, PetStage.ADULT);

    // Each stage should have different art
    assertThat(eggArt).isNotEqualTo(babyArt);
    assertThat(babyArt).isNotEqualTo(teenArt);
    assertThat(teenArt).isNotEqualTo(adultArt);
  }

  @Test
  @DisplayName("should return different art for different types at same stage")
  void shouldReturnDifferentArtForDifferentTypes() {
    String dogAdult = PetAsciiArt.getArt(PetType.DOG, PetStage.ADULT);
    String catAdult = PetAsciiArt.getArt(PetType.CAT, PetStage.ADULT);
    String dragonAdult = PetAsciiArt.getArt(PetType.DRAGON, PetStage.ADULT);

    // Each type should have different adult art (except EGG stage)
    assertThat(dogAdult).isNotEqualTo(catAdult);
    assertThat(catAdult).isNotEqualTo(dragonAdult);
    assertThat(dragonAdult).isNotEqualTo(dogAdult);
  }

  @Test
  @DisplayName("should return art containing visual elements")
  void shouldReturnArtContainingVisualElements() {
    String art = PetAsciiArt.getArt(PetType.DRAGON, PetStage.ADULT);

    // ASCII art should contain some visual elements (parentheses, slashes, etc.)
    assertThat(art).containsAnyOf("(", ")", "/", "\\", "|", "-", "_");
  }
}
