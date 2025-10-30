import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { config } from '@/lib/config';
import { EquipmentSlot } from '@/lib/types';

export function usePetEquipment(petId: string) {
  const queryClient = useQueryClient();

  return useQuery({
    queryKey: ['equipment', petId],
    queryFn: () => api.getPetEquipment(petId),
    refetchInterval: config.pollingInterval,
    enabled: !!petId,
    retry: (failureCount, error) => {
      // Don't retry on 404 errors - pet doesn't exist
      if (error instanceof ApiError && error.isNotFound()) {
        queryClient.removeQueries({ queryKey: ['equipment', petId] });
        return false;
      }
      return failureCount < 3;
    },
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
    onError: (error: Error, variables) => {
      if (error instanceof ApiError && error.isNotFound()) {
        // Pet or item doesn't exist - clean up cache
        queryClient.removeQueries({ queryKey: ['equipment', variables.petId] });
        queryClient.invalidateQueries({ queryKey: ['pets'] });
        queryClient.invalidateQueries({ queryKey: ['equipment', 'inventory'] });
        toast({
          title: 'Item not found',
          description: 'The pet or item no longer exists. Refreshing...',
          variant: 'destructive',
        });
      } else {
        toast({
          title: 'Failed to equip',
          description: error.message,
          variant: 'destructive',
        });
      }
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
    onError: (error: Error, variables) => {
      if (error instanceof ApiError && error.isNotFound()) {
        // Pet doesn't exist - clean up cache
        queryClient.removeQueries({ queryKey: ['equipment', variables.petId] });
        queryClient.invalidateQueries({ queryKey: ['pets'] });
        toast({
          title: 'Pet not found',
          description: 'This pet no longer exists. Refreshing...',
          variant: 'destructive',
        });
      } else {
        toast({
          title: 'Failed to unequip',
          description: error.message,
          variant: 'destructive',
        });
      }
    },
  });
}
