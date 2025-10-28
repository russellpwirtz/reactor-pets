package com.reactor.pets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// CHECKSTYLE:OFF HideUtilityClassConstructor - Not a utility class; Spring needs default constructor
@SpringBootApplication
public class ReactorPetsApplication {
  // CHECKSTYLE:ON HideUtilityClassConstructor

  public static void main(String[] args) {
    SpringApplication.run(ReactorPetsApplication.class, args);
  }
}
