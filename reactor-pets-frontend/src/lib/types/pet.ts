export type PetType = 'DOG' | 'CAT' | 'DRAGON';
export type PetStage = 'EGG' | 'BABY' | 'TEEN' | 'ADULT';
export type EvolutionPath = 'UNDETERMINED' | 'HEALTHY' | 'NEGLECTED';
export type LeaderboardType = 'AGE' | 'HAPPINESS' | 'HEALTH';

export interface Pet {
  petId: string;
  name: string;
  type: PetType;
  stage: PetStage;
  evolutionPath: EvolutionPath;
  isAlive: boolean;
  age: number;
  totalTicks: number;
  hunger: number;
  happiness: number;
  health: number;
  lastUpdated: string;
  asciiArt: string;
}

export interface CreatePetRequest {
  name: string;
  type: PetType;
}

export interface Statistics {
  totalPetsCreated: number;
  totalPetsDied: number;
  currentlyAlive: number;
  averageLifespan: number;
  longestLivedPetName: string;
  longestLivedPetId: string;
  longestLivedPetAge: number;
  stageDistribution: Record<PetStage, number>;
  lastUpdated: string;
}

export interface LeaderboardEntry {
  petId: string;
  name: string;
  type: PetType;
  value: number;
  stage: PetStage;
  isAlive: boolean;
}

export interface PetEvent {
  eventType: string;
  timestamp: string;
  details: string;
}
