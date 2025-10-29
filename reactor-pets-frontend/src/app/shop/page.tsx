'use client';

import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ShopGrid } from '@/components/shop/shop-grid';
import { XPCard } from '@/components/progression/xp-card';
import {
  useShopItems,
  useShopUpgrades,
  usePurchaseEquipment,
  usePurchaseUpgrade,
} from '@/hooks/use-shop';
import { useProgression } from '@/hooks/use-progression';

export default function ShopPage() {
  const { data: items, isLoading: itemsLoading } = useShopItems();
  const { data: upgrades, isLoading: upgradesLoading } = useShopUpgrades();
  const { data: progression } = useProgression();

  const purchaseEquipment = usePurchaseEquipment();
  const purchaseUpgrade = usePurchaseUpgrade();

  const handlePurchaseEquipment = (itemType: string) => {
    purchaseEquipment.mutate(itemType);
  };

  const handlePurchaseUpgrade = (upgradeType: string) => {
    purchaseUpgrade.mutate(upgradeType);
  };

  return (
    <div className="container mx-auto py-8">
      <h1 className="text-4xl font-bold mb-8">Shop</h1>

      {progression && (
        <div className="mb-8">
          <XPCard {...progression} />
        </div>
      )}

      <Tabs defaultValue="equipment" className="mt-8">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="equipment">Equipment</TabsTrigger>
          <TabsTrigger value="upgrades">Permanent Upgrades</TabsTrigger>
        </TabsList>

        <TabsContent value="equipment">
          <ShopGrid
            items={items || []}
            currentXP={progression?.currentXP || 0}
            onPurchase={handlePurchaseEquipment}
            isLoading={itemsLoading}
          />
        </TabsContent>

        <TabsContent value="upgrades">
          <ShopGrid
            items={upgrades || []}
            currentXP={progression?.currentXP || 0}
            onPurchase={handlePurchaseUpgrade}
            isLoading={upgradesLoading}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
