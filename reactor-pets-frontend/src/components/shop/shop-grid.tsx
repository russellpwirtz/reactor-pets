'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Coins } from 'lucide-react';
import { ShopItem } from '@/lib/types/shop';

interface ShopGridProps {
  items: ShopItem[];
  currentXP: number;
  onPurchase: (itemType: string) => void;
  isLoading?: boolean;
}

const itemIcons: Record<string, string> = {
  // Equipment - Food Bowls
  SLOW_FEEDER: '🍽️',
  NUTRIENT_BOWL: '🍲',
  AUTO_FEEDER: '🏆',
  // Equipment - Toys
  TOY_BOX: '🎾',
  EXERCISE_WHEEL: '⚽',
  ENTERTAINMENT_SYSTEM: '🎮',
  // Equipment - Accessories
  COZY_BED: '🛏️',
  XP_CHARM: '📿',
  HEALTH_MONITOR: '💚',
  // Permanent Upgrades
  EFFICIENT_METABOLISM: '⚡',
  HAPPY_DISPOSITION: '😊',
  STURDY_GENETICS: '🧬',
  INDUSTRIAL_KITCHEN: '👨‍🍳',
  FAST_HATCHER: '🥚',
  MULTI_PET_LICENSE_I: '🎟️',
  MULTI_PET_LICENSE_II: '🎫',
  MULTI_PET_LICENSE_III: '🏅',
  // Consumables (for future phase)
  APPLE: '🍎',
  PIZZA: '🍕',
  GOURMET_MEAL: '🍽️',
  BASIC_MEDICINE: '💊',
  ADVANCED_MEDICINE: '🏥',
  COOKIE: '🍪',
  PREMIUM_TOY: '🧸',
};

export function ShopGrid({ items, currentXP, onPurchase, isLoading }: ShopGridProps) {
  if (isLoading) {
    return <div className="text-center py-8">Loading items...</div>;
  }

  if (items.length === 0) {
    return (
      <div className="text-center text-muted-foreground py-8">
        No items available
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mt-6">
      {items.map((item) => {
        const canAfford = currentXP >= item.xpCost;

        return (
          <Card key={item.itemId} className="hover:shadow-lg transition-shadow">
            <CardHeader>
              <div className="text-center text-4xl mb-2">
                {itemIcons[item.itemId] || '📦'}
              </div>
              <CardTitle className="text-center text-base">
                {item.name}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-sm text-muted-foreground text-center min-h-[40px]">
                {item.description}
              </p>

              {item.equipmentSlot && (
                <div className="text-center">
                  <Badge variant="outline" className="text-xs">
                    {item.equipmentSlot.replace(/_/g, ' ')}
                  </Badge>
                </div>
              )}

              <div className="flex items-center justify-center gap-2">
                <Coins className="h-4 w-4 text-yellow-500" />
                <Badge variant={canAfford ? 'default' : 'secondary'}>
                  {item.xpCost} XP
                </Badge>
              </div>

              <Button
                className="w-full"
                onClick={() => onPurchase(item.itemId)}
                disabled={!canAfford}
              >
                {canAfford ? 'Purchase' : 'Not Enough XP'}
              </Button>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
