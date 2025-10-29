export type ItemCategory = 'EQUIPMENT' | 'PERMANENT_UPGRADE' | 'CONSUMABLE';

export interface ShopItem {
  itemType: string;
  category: ItemCategory;
  xpCost: number;
  description: string;
  slot?: string;
}
