export type UpgradeType =
  | 'EFFICIENT_METABOLISM'
  | 'HAPPY_DISPOSITION'
  | 'STURDY_GENETICS'
  | 'INDUSTRIAL_KITCHEN'
  | 'FAST_HATCHER'
  | 'MULTI_PET_LICENSE_I'
  | 'MULTI_PET_LICENSE_II'
  | 'MULTI_PET_LICENSE_III';

export interface PlayerProgression {
  playerId: string;
  currentXP: number;
  lifetimeXP: number;
  xpMultiplier: number;
  highestMultiplier: number;
  totalXPSpent: number;
  permanentUpgrades: UpgradeType[];
}
