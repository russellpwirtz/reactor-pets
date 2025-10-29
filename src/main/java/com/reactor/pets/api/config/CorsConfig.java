package com.reactor.pets.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for allowing frontend applications to access the REST API.
 * Configured for local development with Next.js (port 3000) and Vite (port 5173).
 */
@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allow localhost origins for development
    configuration.addAllowedOrigin("http://localhost:3000"); // Next.js dev server
    configuration.addAllowedOrigin("http://localhost:5173"); // Vite dev server
    configuration.addAllowedOrigin("http://localhost:8080"); // Alternative dev server

    // Allow all HTTP methods
    configuration.addAllowedMethod("GET");
    configuration.addAllowedMethod("POST");
    configuration.addAllowedMethod("PUT");
    configuration.addAllowedMethod("DELETE");
    configuration.addAllowedMethod("OPTIONS");

    // Allow all headers
    configuration.addAllowedHeader("*");

    // Allow credentials (cookies, authorization headers)
    configuration.setAllowCredentials(true);

    // How long the browser should cache the CORS preflight response (in seconds)
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);

    return source;
  }
}
