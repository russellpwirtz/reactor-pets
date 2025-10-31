package com.reactor.pets.config;

import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Axon Framework configuration for event sourcing and snapshotting.
 *
 * Configures snapshot triggers for aggregates to optimize performance by reducing
 * the number of events that need to be replayed when loading aggregate state.
 */
@Configuration
public class AxonConfig {

  /**
   * Configures snapshot trigger for the Pet aggregate.
   *
   * Snapshots are created every 50 events to balance performance optimization
   * with storage overhead. This is particularly important for Pet aggregates which
   * can accumulate hundreds or thousands of events through:
   * - Time tick events (every global tick)
   * - User interactions (feeding, playing, cleaning)
   * - Equipment changes (equip/unequip items)
   * - Consumable usage
   * - Health deterioration and evolution events
   *
   * @param snapshotter the Axon snapshotter component
   * @return snapshot trigger definition configured for 50 events
   */
  @Bean
  public SnapshotTriggerDefinition petSnapshotTrigger(Snapshotter snapshotter) {
    return new EventCountSnapshotTriggerDefinition(snapshotter, 50);
  }
}
