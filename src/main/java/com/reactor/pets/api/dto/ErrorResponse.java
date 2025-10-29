package com.reactor.pets.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response for API failures")
public class ErrorResponse {

  @Schema(description = "Error type or category", example = "PET_NOT_FOUND")
  private String error;

  @Schema(description = "Detailed error message", example = "No pet exists with ID abc123")
  private String message;

  @Schema(description = "HTTP status code", example = "404")
  private int status;

  @Schema(description = "Timestamp when the error occurred")
  private Instant timestamp;

  @Schema(description = "Request path that caused the error", example = "/api/pets/abc123")
  private String path;
}
