'use client';

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { EquipmentItem, EquipmentSlot } from '@/lib/types/equipment';

interface EquipmentInventoryDialogProps {
  open: boolean;
  onClose: () => void;
  slot: EquipmentSlot | null;
  items: EquipmentItem[];
  onEquipItem: (itemId: string) => void;
}

const itemIcons: Record<string, string> = {
  BASIC_BOWL: '🍽️',
  LARGE_BOWL: '🍲',
  PREMIUM_BOWL: '🏆',
  SIMPLE_BALL: '⚽',
  INTERACTIVE_TOY: '🎮',
  LUXURY_TOY_SET: '🎁',
  BASIC_COLLAR: '📿',
  COMFORT_BED: '🛏️',
  HEALTH_MONITOR: '💚',
};

function formatModifierKey(key: string): string {
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/Modifier/g, '')
    .replace(/Efficiency/g, 'Eff.')
    .trim();
}

export function EquipmentInventoryDialog({
  open,
  onClose,
  slot,
  items,
  onEquipItem,
}: EquipmentInventoryDialogProps) {
  const availableItems = items.filter((item) => item.slot === slot);

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Select {slot?.replace('_', ' ')}</DialogTitle>
        </DialogHeader>
        <div className="space-y-2 max-h-96 overflow-y-auto">
          {availableItems.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">
              No items available for this slot. Visit the shop to purchase equipment!
            </p>
          ) : (
            availableItems.map((item) => (
              <div
                key={item.itemId}
                className="flex items-center justify-between p-3 border rounded-lg hover:bg-accent transition-colors"
              >
                <div className="flex items-center gap-3 flex-1">
                  <span className="text-2xl">{itemIcons[item.equipmentType] || '📦'}</span>
                  <div>
                    <p className="font-medium">{item.equipmentType.replace(/_/g, ' ')}</p>
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
                </div>
                <Button size="sm" onClick={() => onEquipItem(item.itemId)}>
                  Equip
                </Button>
              </div>
            ))
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
