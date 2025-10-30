import { useQueries } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { config } from '@/lib/config';
import { usePets } from './use-pets';
import type { EquippedItems } from '@/lib/types';

export function useAllPetEquipment() {
  const { data: pets } = usePets();

  const equipmentQueries = useQueries({
    queries:
      pets?.map((pet) => ({
        queryKey: ['equipment', pet.petId],
        queryFn: () => api.getPetEquipment(pet.petId),
        refetchInterval: config.pollingInterval,
        enabled: !!pet.petId && pet.alive,
      })) ?? [],
  });

  // Build a map of itemId -> { petId, petName, slot }
  const equippedItemsMap = new Map<
    string,
    { petId: string; petName: string; slot: string }
  >();

  equipmentQueries.forEach((query, index) => {
    const pet = pets?.[index];
    if (query.data && pet) {
      const equipment = query.data as EquippedItems;
      if (equipment.foodBowl) {
        equippedItemsMap.set(equipment.foodBowl.itemId, {
          petId: pet.petId,
          petName: pet.name,
          slot: 'FOOD_BOWL',
        });
      }
      if (equipment.toy) {
        equippedItemsMap.set(equipment.toy.itemId, {
          petId: pet.petId,
          petName: pet.name,
          slot: 'TOY',
        });
      }
      if (equipment.accessory) {
        equippedItemsMap.set(equipment.accessory.itemId, {
          petId: pet.petId,
          petName: pet.name,
          slot: 'ACCESSORY',
        });
      }
    }
  });

  return {
    equippedItemsMap,
    isLoading: equipmentQueries.some((q) => q.isLoading),
  };
}
