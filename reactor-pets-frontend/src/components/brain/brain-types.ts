/**
 * Brain visualization type definitions
 * Matches backend CellState model from PetBrainSimulator
 */

export type CellType = 'EXCITATORY' | 'INHIBITORY';

export type CorticalLayer = 'LAYER_2_3' | 'LAYER_4' | 'LAYER_5' | 'LAYER_6';

export type NeuronPhase =
  | 'RESTING'
  | 'DEPOLARIZING'
  | 'REPOLARIZING'
  | 'HYPERPOLARIZED'
  | 'RECOVERING'
  | 'BURSTING';

export interface CellState {
  x: number;
  y: number;
  activation: number;
  isFiring: boolean;
  cellType: CellType;
  layer: CorticalLayer;
  phase: NeuronPhase;
  refractoryCounter: number;
}

export interface GridConfig {
  width: number;
  height: number;
}
