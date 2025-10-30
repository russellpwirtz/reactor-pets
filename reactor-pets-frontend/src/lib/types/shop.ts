export type ItemCategory = 'EQUIPMENT' | 'PERMANENT_UPGRADE' | 'CONSUMABLE';

export interface ShopItem {
  itemId: string;
  name: string;
  description: string;
  itemType: ItemCategory;
  xpCost: number;
  equipmentSlot?: string;
  upgradeType?: string;
}
