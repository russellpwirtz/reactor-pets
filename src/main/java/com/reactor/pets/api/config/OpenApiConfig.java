package com.reactor.pets.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for API documentation.
 * API documentation will be available at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI reactorPetsOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Reactor Pets API")
                .description(
                    "REST API for managing virtual pets with Axon Framework and Project Reactor")
                .version("1.0.0")
                .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
        .externalDocs(
            new ExternalDocumentation()
                .description("Project Documentation")
                .url("https://github.com/yourusername/reactor-pets"));
  }
}
