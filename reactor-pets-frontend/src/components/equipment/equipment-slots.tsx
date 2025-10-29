'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { EquipmentItem, EquipmentSlot } from '@/lib/types/equipment';

interface EquipmentSlotsProps {
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
  foodBowl,
  toy,
  accessory,
  onEquip,
  onUnequip,
}: EquipmentSlotsProps) {
  const slots: Array<{ slot: EquipmentSlot; item: EquipmentItem | null; label: string }> = [
    { slot: 'FOOD_BOWL', item: foodBowl, label: slotLabels.FOOD_BOWL },
    { slot: 'TOY', item: toy, label: slotLabels.TOY },
    { slot: 'ACCESSORY', item: accessory, label: slotLabels.ACCESSORY },
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Equipment</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {slots.map(({ slot, item, label }) => (
          <div key={slot} className="flex items-center justify-between p-3 border rounded-lg">
            <div className="flex items-center gap-3 flex-1">
              <span className="text-2xl">{slotIcons[slot]}</span>
              <div className="flex-1">
                <p className="text-sm font-medium">{label}</p>
                {item ? (
                  <div className="mt-1">
                    <p className="text-xs text-muted-foreground">
                      {item.equipmentType.replace(/_/g, ' ')}
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
                  <p className="text-xs text-muted-foreground">Empty</p>
                )}
              </div>
            </div>
            {item ? (
              <Button size="sm" variant="outline" onClick={() => onUnequip(slot)}>
                Unequip
              </Button>
            ) : (
              <Button size="sm" onClick={() => onEquip(slot)}>
                Equip
              </Button>
            )}
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
