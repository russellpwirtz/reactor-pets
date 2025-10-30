import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { useEquipmentInventory } from '@/hooks/use-equipment';
import { useAllPetEquipment } from '@/hooks/use-all-pet-equipment';
import type { EquipmentItem } from '@/lib/types';
import Link from 'next/link';

export function InventoryCard() {
  const { data: inventory } = useEquipmentInventory();
  const { equippedItemsMap } = useAllPetEquipment();

  const getEquipmentSlotLabel = (slot: string): string => {
    switch (slot) {
      case 'FOOD_BOWL':
        return 'Food Bowl';
      case 'TOY':
        return 'Toy';
      case 'ACCESSORY':
        return 'Accessory';
      default:
        return slot;
    }
  };

  const getModifierDescription = (item: EquipmentItem): string => {
    const modifiers = [];
    const mods = item.modifiers;

    if (mods.hungerDecayModifier) {
      modifiers.push(`${(mods.hungerDecayModifier * 100).toFixed(0)}% Hunger Decay`);
    }
    if (mods.happinessDecayModifier) {
      modifiers.push(`${(mods.happinessDecayModifier * 100).toFixed(0)}% Happiness Decay`);
    }
    if (mods.healthDecayModifier) {
      modifiers.push(`${(mods.healthDecayModifier * 100).toFixed(0)}% Health Decay`);
    }
    if (mods.foodEfficiency) {
      modifiers.push(`+${(mods.foodEfficiency * 100).toFixed(0)}% Food Efficiency`);
    }
    if (mods.playEfficiency) {
      modifiers.push(`+${(mods.playEfficiency * 100).toFixed(0)}% Play Efficiency`);
    }

    return modifiers.join(', ') || 'No modifiers';
  };

  const getItemName = (equipmentType: string): string => {
    // Convert BASIC_BOWL to "Basic Bowl"
    return equipmentType
      .split('_')
      .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
      .join(' ');
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>Equipment Inventory</CardTitle>
          <Link href="/shop">
            <Badge variant="outline" className="cursor-pointer hover:bg-accent">
              Visit Shop
            </Badge>
          </Link>
        </div>
      </CardHeader>
      <CardContent>
        {!inventory || inventory.items.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No equipment in inventory. Visit the shop to purchase equipment!
          </p>
        ) : (
          <div className="space-y-2">
            {inventory.items.map((item) => {
              const equippedInfo = equippedItemsMap.get(item.itemId);
              const isEquipped = !!equippedInfo;

              return (
                <div
                  key={item.itemId}
                  className={`flex items-start justify-between p-3 rounded-lg border ${
                    isEquipped ? 'bg-accent/30 border-primary/50' : 'bg-card'
                  }`}
                >
                  <div className="space-y-1 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium">{getItemName(item.name)}</span>
                      <Badge variant="secondary" className="text-xs">
                        {getEquipmentSlotLabel(item.slot)}
                      </Badge>
                      {isEquipped && equippedInfo && (
                        <Badge variant="default" className="text-xs">
                          Equipped on {equippedInfo.petName}
                        </Badge>
                      )}
                    </div>
                    <p className="text-sm text-muted-foreground">{getModifierDescription(item)}</p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
