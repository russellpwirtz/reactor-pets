import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '@/lib/api';
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
      let description = error.message;

      if (error instanceof ApiError) {
        if (error.status === 400) {
          description = 'Insufficient XP or item unavailable';
        } else if (error.isNotFound()) {
          description = 'Item not found in shop';
        }
      }

      toast({
        title: 'Purchase failed',
        description,
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
      let description = error.message;

      if (error instanceof ApiError) {
        if (error.status === 400) {
          description = 'Insufficient XP or upgrade already owned';
        } else if (error.isNotFound()) {
          description = 'Upgrade not found in shop';
        }
      }

      toast({
        title: 'Purchase failed',
        description,
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
      let description = error.message;

      if (error instanceof ApiError) {
        if (error.status === 400) {
          description = 'Insufficient XP or item unavailable';
        } else if (error.isNotFound()) {
          description = 'Item not found in shop';
        }
      }

      toast({
        title: 'Purchase failed',
        description,
        variant: 'destructive',
      });
    },
  });
}
