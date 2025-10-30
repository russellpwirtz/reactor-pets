package com.reactor.pets.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reactor.pets.event.GlobalTimeAdvancedEvent;
import com.reactor.pets.event.GlobalTimeCreatedEvent;
import com.reactor.pets.query.GetGlobalTimeQuery;
import com.reactor.pets.query.GlobalTimeRepository;
import com.reactor.pets.query.GlobalTimeView;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for GlobalTimeProjection.
 */
@DisplayName("Global Time Projection")
@ExtendWith(MockitoExtension.class)
class GlobalTimeProjectionTest {

  @Mock
  private GlobalTimeRepository globalTimeRepository;

  private GlobalTimeProjection projection;

  @BeforeEach
  void setUp() {
    projection = new GlobalTimeProjection(globalTimeRepository);
  }

  @Test
  @DisplayName("should create global time view when GlobalTimeCreatedEvent is received")
  void shouldCreateGlobalTimeView() {
    // Given
    String timeId = "GLOBAL_TIME";
    Instant timestamp = Instant.now();
    GlobalTimeCreatedEvent event = new GlobalTimeCreatedEvent(timeId, timestamp);

    // When
    projection.on(event);

    // Then
    ArgumentCaptor<GlobalTimeView> captor = ArgumentCaptor.forClass(GlobalTimeView.class);
    verify(globalTimeRepository).save(captor.capture());

    GlobalTimeView savedView = captor.getValue();
    assertThat(savedView.getTimeId()).isEqualTo(timeId);
    assertThat(savedView.getCurrentGlobalTick()).isEqualTo(0L);
    assertThat(savedView.getLastUpdated()).isEqualTo(timestamp);
  }

  @Test
  @DisplayName("should update global tick when GlobalTimeAdvancedEvent is received")
  void shouldUpdateGlobalTick() {
    // Given
    String timeId = "GLOBAL_TIME";
    long newTick = 42L;
    Instant timestamp = Instant.now();
    GlobalTimeAdvancedEvent event = new GlobalTimeAdvancedEvent(timeId, newTick, timestamp);

    GlobalTimeView existingView = new GlobalTimeView();
    existingView.setTimeId(timeId);
    existingView.setCurrentGlobalTick(41L);
    existingView.setLastUpdated(Instant.now().minusSeconds(10));

    when(globalTimeRepository.findById(timeId)).thenReturn(Optional.of(existingView));

    // When
    projection.on(event);

    // Then
    ArgumentCaptor<GlobalTimeView> captor = ArgumentCaptor.forClass(GlobalTimeView.class);
    verify(globalTimeRepository).save(captor.capture());

    GlobalTimeView updatedView = captor.getValue();
    assertThat(updatedView.getCurrentGlobalTick()).isEqualTo(newTick);
    assertThat(updatedView.getLastUpdated()).isEqualTo(timestamp);
  }

  @Test
  @DisplayName("should handle GlobalTimeAdvancedEvent when time does not exist")
  void shouldHandleAdvanceEventForNonExistentTime() {
    // Given
    String timeId = "GLOBAL_TIME";
    GlobalTimeAdvancedEvent event = new GlobalTimeAdvancedEvent(timeId, 100L, Instant.now());

    when(globalTimeRepository.findById(timeId)).thenReturn(Optional.empty());

    // When
    projection.on(event);

    // Then - should not throw exception, just skip
    verify(globalTimeRepository).findById(timeId);
  }

  @Test
  @DisplayName("should return global time view when it exists")
  void shouldReturnGlobalTimeView() {
    // Given
    GlobalTimeView view = new GlobalTimeView();
    view.setTimeId("GLOBAL_TIME");
    view.setCurrentGlobalTick(123L);
    view.setLastUpdated(Instant.now());

    when(globalTimeRepository.findAll()).thenReturn(List.of(view));

    // When
    GlobalTimeView result = projection.handle(new GetGlobalTimeQuery());

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTimeId()).isEqualTo("GLOBAL_TIME");
    assertThat(result.getCurrentGlobalTick()).isEqualTo(123L);
  }

  @Test
  @DisplayName("should return null when global time does not exist")
  void shouldReturnNullWhenNotExists() {
    // Given
    when(globalTimeRepository.findAll()).thenReturn(Collections.emptyList());

    // When
    GlobalTimeView result = projection.handle(new GetGlobalTimeQuery());

    // Then
    assertThat(result).isNull();
  }
}
