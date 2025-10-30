'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { EquipmentItem, EquipmentSlot } from '@/lib/types/equipment';
import { Pet } from '@/lib/types';

interface EquipmentSlotsProps {
  pet: Pet;
  foodBowl: EquipmentItem | null;
  toy: EquipmentItem | null;
  accessory: EquipmentItem | null;
  onEquip: (slot: EquipmentSlot) => void;
  onUnequip: (slot: EquipmentSlot) => void;
}

const slotIcons: Record<EquipmentSlot, string> = {
  FOOD_BOWL: '🍽️',
  TOY: '🎾',
  ACCESSORY: '📿',
};

const slotLabels: Record<EquipmentSlot, string> = {
  FOOD_BOWL: 'Food Bowl',
  TOY: 'Toy',
  ACCESSORY: 'Accessory',
};

function formatModifierKey(key: string): string {
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/Modifier/g, '')
    .replace(/Efficiency/g, 'Eff.')
    .trim();
}

export function EquipmentSlots({
  pet,
  foodBowl,
  toy,
  accessory,
  onEquip,
  onUnequip,
}: EquipmentSlotsProps) {
  const slots: Array<{
    slot: EquipmentSlot;
    item: EquipmentItem | null;
    label: string;
    slotIndex: number;
  }> = [
    { slot: 'FOOD_BOWL', item: foodBowl, label: slotLabels.FOOD_BOWL, slotIndex: 0 },
    { slot: 'TOY', item: toy, label: slotLabels.TOY, slotIndex: 1 },
    { slot: 'ACCESSORY', item: accessory, label: slotLabels.ACCESSORY, slotIndex: 2 },
  ];

  // Count currently equipped items
  const equippedCount = [foodBowl, toy, accessory].filter(Boolean).length;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <span>Equipment</span>
          <span className="text-sm font-normal text-muted-foreground">
            {equippedCount}/{pet.maxEquipmentSlots} slots used
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {pet.maxEquipmentSlots === 0 && (
          <div className="text-sm text-muted-foreground text-center py-4 border rounded-lg bg-muted/30">
            Equipment slots unlock when your pet evolves from EGG to BABY stage.
          </div>
        )}
        {slots.map(({ slot, item, label, slotIndex }) => {
          const isSlotAvailable = slotIndex < pet.maxEquipmentSlots;
          const isSlotLocked = !isSlotAvailable;
          const canEquip = isSlotAvailable && !item;
          const canUnequip = isSlotAvailable && !!item;

          return (
            <div
              key={slot}
              className={`flex items-center justify-between p-3 border rounded-lg ${
                isSlotLocked ? 'opacity-50 bg-muted/30' : ''
              }`}
            >
              <div className="flex items-center gap-3 flex-1">
                <span className="text-2xl">{slotIcons[slot]}</span>
                <div className="flex-1">
                  <p className="text-sm font-medium">
                    {label}
                    {isSlotLocked && <span className="ml-2 text-xs text-muted-foreground">🔒 Locked</span>}
                  </p>
                  {item ? (
                    <div className="mt-1">
                      <p className="text-xs text-muted-foreground">
                        {item.name.replace(/_/g, ' ')}
                      </p>
                      <div className="flex gap-1 mt-1 flex-wrap">
                        {Object.entries(item.modifiers).map(([key, value]) => (
                          <Badge key={key} variant="outline" className="text-xs">
                            {formatModifierKey(key)}: {value > 0 ? '+' : ''}
                            {value}
                            {key.includes('Efficiency') ? '%' : ''}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <p className="text-xs text-muted-foreground">
                      {isSlotLocked ? 'Unlocks at stage ' + getUnlockStage(slotIndex) : 'Empty'}
                    </p>
                  )}
                </div>
              </div>
              {canUnequip ? (
                <Button size="sm" variant="outline" onClick={() => onUnequip(slot)}>
                  Unequip
                </Button>
              ) : canEquip ? (
                <Button size="sm" onClick={() => onEquip(slot)}>
                  Equip
                </Button>
              ) : (
                <Button size="sm" disabled>
                  {isSlotLocked ? 'Locked' : 'Empty'}
                </Button>
              )}
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}

function getUnlockStage(slotIndex: number): string {
  // Baby=1, Teen=2, Adult=3
  switch (slotIndex) {
    case 0:
      return 'BABY';
    case 1:
      return 'TEEN';
    case 2:
      return 'ADULT';
    default:
      return 'UNKNOWN';
  }
}
