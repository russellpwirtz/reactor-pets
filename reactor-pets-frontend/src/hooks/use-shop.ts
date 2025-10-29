import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';

export function useShopItems() {
  return useQuery({
    queryKey: ['shop', 'items'],
    queryFn: api.getShopItems,
  });
}

export function useShopUpgrades() {
  return useQuery({
    queryKey: ['shop', 'upgrades'],
    queryFn: api.getShopUpgrades,
  });
}

export function usePurchaseEquipment() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: api.purchaseEquipment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['progression'] });
      queryClient.invalidateQueries({ queryKey: ['equipment'] });
      toast({
        title: 'Purchase successful!',
        description: 'Equipment has been added to your inventory.',
      });
    },
    onError: (error: Error) => {
      toast({
        title: 'Purchase failed',
        description: error.message,
        variant: 'destructive',
      });
    },
  });
}

export function usePurchaseUpgrade() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: api.purchaseUpgrade,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['progression'] });
      toast({
        title: 'Purchase successful!',
        description: 'Permanent upgrade has been applied.',
      });
    },
    onError: (error: Error) => {
      toast({
        title: 'Purchase failed',
        description: error.message,
        variant: 'destructive',
      });
    },
  });
}

export function usePurchaseConsumable() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: api.purchaseConsumable,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['progression'] });
      queryClient.invalidateQueries({ queryKey: ['consumables'] });
      toast({
        title: 'Purchase successful!',
        description: 'Consumable has been added to your inventory.',
      });
    },
    onError: (error: Error) => {
      toast({
        title: 'Purchase failed',
        description: error.message,
        variant: 'destructive',
      });
    },
  });
}
