package com.reactor.pets.domain;

/**
 * Types of stat modifiers that equipment can provide.
 * These modifiers affect various pet stats and behaviors.
 */
public enum StatModifier {
  /** Modifies the rate at which hunger increases over time */
  HUNGER_DECAY_RATE,

  /** Modifies the rate at which happiness decreases over time */
  HAPPINESS_DECAY_RATE,

  /** Modifies the rate at which health decreases over time */
  HEALTH_DECAY_RATE,

  /** Modifies the effectiveness of feeding actions */
  FOOD_EFFICIENCY,

  /** Modifies the effectiveness of playing actions */
  PLAY_EFFICIENCY,

  /** Provides passive health regeneration per tick */
  HEALTH_REGEN
}
