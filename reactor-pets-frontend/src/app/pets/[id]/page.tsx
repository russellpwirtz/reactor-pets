'use client';

import { use, useState } from 'react';
import { usePet } from '@/hooks/use-pets';
import { PetDetailView } from '@/components/pets/pet-detail-view';
import { PetDetailSkeleton } from '@/components/pets/pet-detail-skeleton';
import { PetHistory } from '@/components/pets/pet-history';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import { EquipmentSlots } from '@/components/equipment/equipment-slots';
import { EquipmentInventoryDialog } from '@/components/equipment/equipment-inventory-dialog';
import { usePetEquipment, useEquipmentInventory, useEquipItem, useUnequipItem } from '@/hooks/use-equipment';
import { EquipmentSlot } from '@/lib/types';

export default function PetDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { data: pet, isLoading } = usePet(id);
  const { data: equipment } = usePetEquipment(id);
  const { data: inventory } = useEquipmentInventory();
  const equipItem = useEquipItem();
  const unequipItem = useUnequipItem();

  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState<EquipmentSlot | null>(null);

  const handleEquipClick = (slot: EquipmentSlot) => {
    setSelectedSlot(slot);
    setIsDialogOpen(true);
  };

  const handleUnequip = (slot: EquipmentSlot) => {
    unequipItem.mutate({ petId: id, slot });
  };

  const handleEquipItem = (itemId: string) => {
    if (selectedSlot) {
      equipItem.mutate({ petId: id, slot: selectedSlot, itemId });
      setIsDialogOpen(false);
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-8">
        <div className="mb-6">
          <Link href="/pets">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Pets
            </Button>
          </Link>
        </div>
        <PetDetailSkeleton />
      </div>
    );
  }

  if (!pet) {
    return (
      <div className="container mx-auto py-8">
        <div className="text-center py-12 text-red-500">
          Pet not found
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8">
      <div className="mb-6">
        <Link href="/pets">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Pets
          </Button>
        </Link>
      </div>

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="equipment">Equipment</TabsTrigger>
          <TabsTrigger value="history">History</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-6">
          <PetDetailView pet={pet} />
        </TabsContent>

        <TabsContent value="equipment" className="mt-6">
          {equipment && pet && (
            <>
              <EquipmentSlots
                pet={pet}
                {...equipment}
                onEquip={handleEquipClick}
                onUnequip={handleUnequip}
              />
              <EquipmentInventoryDialog
                open={isDialogOpen}
                onClose={() => setIsDialogOpen(false)}
                slot={selectedSlot}
                items={inventory?.items || []}
                onEquipItem={handleEquipItem}
              />
            </>
          )}
        </TabsContent>

        <TabsContent value="history" className="mt-6">
          <PetHistory petId={pet.petId} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
