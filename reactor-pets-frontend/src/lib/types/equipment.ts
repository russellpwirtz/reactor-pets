export type EquipmentSlot = 'FOOD_BOWL' | 'TOY' | 'ACCESSORY';

export type EquipmentType =
  | 'BASIC_BOWL'
  | 'LARGE_BOWL'
  | 'PREMIUM_BOWL'
  | 'SIMPLE_BALL'
  | 'INTERACTIVE_TOY'
  | 'LUXURY_TOY_SET'
  | 'BASIC_COLLAR'
  | 'COMFORT_BED'
  | 'HEALTH_MONITOR';

export interface EquipmentModifiers {
  hungerDecayModifier?: number;
  happinessDecayModifier?: number;
  healthDecayModifier?: number;
  foodEfficiency?: number;
  playEfficiency?: number;
}

export interface EquipmentItem {
  itemId: string;
  name: string;
  slot: EquipmentSlot;
  modifiers: EquipmentModifiers;
}

export interface EquippedItems {
  petId: string;
  foodBowl: EquipmentItem | null;
  toy: EquipmentItem | null;
  accessory: EquipmentItem | null;
}

export interface EquipmentInventory {
  items: EquipmentItem[];
}

export interface EquipRequest {
  petId: string;
  slot: EquipmentSlot;
  itemId: string;
}

export interface UnequipRequest {
  petId: string;
  slot: EquipmentSlot;
}
