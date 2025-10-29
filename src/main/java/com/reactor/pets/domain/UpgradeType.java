package com.reactor.pets.domain;

/**
 * Types of permanent upgrades that affect gameplay.
 */
public enum UpgradeType {
  /** Reduces hunger decay rate for all pets */
  EFFICIENT_METABOLISM,

  /** Reduces happiness decay rate for all pets */
  HAPPY_DISPOSITION,

  /** Reduces health decay rate for all pets */
  STURDY_GENETICS,

  /** Increases food effectiveness for all pets */
  INDUSTRIAL_KITCHEN,

  /** Reduces hatching time for eggs */
  FAST_HATCHER,

  /** Increases max pet limit to 2 */
  MULTI_PET_LICENSE_I,

  /** Increases max pet limit to 3 */
  MULTI_PET_LICENSE_II,

  /** Increases max pet limit to 4 */
  MULTI_PET_LICENSE_III
}
