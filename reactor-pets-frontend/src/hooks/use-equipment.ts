import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { config } from '@/lib/config';
import { EquipmentSlot } from '@/lib/types';

export function usePetEquipment(petId: string) {
  return useQuery({
    queryKey: ['equipment', petId],
    queryFn: () => api.getPetEquipment(petId),
    refetchInterval: config.pollingInterval,
    enabled: !!petId,
  });
}

export function useEquipmentInventory() {
  return useQuery({
    queryKey: ['equipment', 'inventory'],
    queryFn: api.getEquipmentInventory,
  });
}

export function useEquipItem() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ petId, slot, itemId }: { petId: string; slot: EquipmentSlot; itemId: string }) =>
      api.equipItem(petId, slot, itemId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['equipment', variables.petId] });
      queryClient.invalidateQueries({ queryKey: ['equipment', 'inventory'] });
      queryClient.invalidateQueries({ queryKey: ['pets', variables.petId] });
      toast({
        title: 'Equipment equipped!',
        description: 'Your pet is now wearing the new equipment.',
      });
    },
    onError: (error: Error) => {
      toast({
        title: 'Failed to equip',
        description: error.message,
        variant: 'destructive',
      });
    },
  });
}

export function useUnequipItem() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: ({ petId, slot }: { petId: string; slot: EquipmentSlot }) =>
      api.unequipItem(petId, slot),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['equipment', variables.petId] });
      queryClient.invalidateQueries({ queryKey: ['equipment', 'inventory'] });
      queryClient.invalidateQueries({ queryKey: ['pets', variables.petId] });
      toast({
        title: 'Equipment unequipped',
        description: 'The item has been returned to your inventory.',
      });
    },
    onError: (error: Error) => {
      toast({
        title: 'Failed to unequip',
        description: error.message,
        variant: 'destructive',
      });
    },
  });
}
