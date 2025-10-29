package com.reactor.pets.api.exception;

import com.reactor.pets.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.concurrent.CompletionException;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn("IllegalArgumentException: {}", ex.getMessage());

    ErrorResponse error =
        ErrorResponse.builder()
            .error("INVALID_ARGUMENT")
            .message(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(AggregateNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAggregateNotFoundException(
      AggregateNotFoundException ex, HttpServletRequest request) {
    log.warn("Pet not found: {}", ex.getMessage());

    ErrorResponse error =
        ErrorResponse.builder()
            .error("PET_NOT_FOUND")
            .message("The requested pet does not exist")
            .status(HttpStatus.NOT_FOUND.value())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(CommandExecutionException.class)
  public ResponseEntity<ErrorResponse> handleCommandExecutionException(
      CommandExecutionException ex, HttpServletRequest request) {
    log.error("Command execution failed: {}", ex.getMessage());

    // Check if the cause is an IllegalStateException (e.g., pet is dead)
    Throwable cause = ex.getCause();
    if (cause instanceof IllegalStateException) {
      ErrorResponse error =
          ErrorResponse.builder()
              .error("INVALID_STATE")
              .message(cause.getMessage())
              .status(HttpStatus.BAD_REQUEST.value())
              .timestamp(Instant.now())
              .path(request.getRequestURI())
              .build();

      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Check if the cause is IllegalArgumentException (e.g., pet not found)
    if (cause instanceof IllegalArgumentException) {
      ErrorResponse error =
          ErrorResponse.builder()
              .error("INVALID_ARGUMENT")
              .message(cause.getMessage())
              .status(HttpStatus.BAD_REQUEST.value())
              .timestamp(Instant.now())
              .path(request.getRequestURI())
              .build();

      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Generic command execution error
    ErrorResponse error =
        ErrorResponse.builder()
            .error("COMMAND_EXECUTION_FAILED")
            .message("Failed to execute command: " + ex.getMessage())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(CompletionException.class)
  public ResponseEntity<ErrorResponse> handleCompletionException(
      CompletionException ex, HttpServletRequest request) {
    log.error("CompletionException: {}", ex.getMessage());

    // Unwrap the cause and handle it appropriately
    Throwable cause = ex.getCause();

    if (cause instanceof CommandExecutionException) {
      return handleCommandExecutionException((CommandExecutionException) cause, request);
    }

    if (cause instanceof IllegalArgumentException) {
      return handleIllegalArgumentException((IllegalArgumentException) cause, request);
    }

    if (cause instanceof AggregateNotFoundException) {
      return handleAggregateNotFoundException((AggregateNotFoundException) cause, request);
    }

    // Generic error
    ErrorResponse error =
        ErrorResponse.builder()
            .error("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    log.warn("Validation error: {}", ex.getMessage());

    StringBuilder messageBuilder = new StringBuilder("Validation failed: ");
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      messageBuilder
          .append(error.getField())
          .append(" - ")
          .append(error.getDefaultMessage())
          .append("; ");
    }

    ErrorResponse error =
        ErrorResponse.builder()
            .error("VALIDATION_ERROR")
            .message(messageBuilder.toString())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {
    log.error("Unexpected error occurred", ex);

    ErrorResponse error =
        ErrorResponse.builder()
            .error("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred: " + ex.getMessage())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
