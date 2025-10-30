import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '@/lib/api/client';
import { config } from '@/lib/config';
import type { CreatePetRequest } from '@/lib/types';
import { useToast } from '@/hooks/use-toast';

export function usePets() {
  return useQuery({
    queryKey: ['pets'],
    queryFn: api.getAllPets,
    refetchInterval: config.pollingInterval,
    retry: (failureCount, error) => {
      // Don't retry on 404 errors
      if (error instanceof ApiError && error.isNotFound()) {
        return false;
      }
      // Retry server errors up to 3 times
      return failureCount < 3;
    },
  });
}

export function usePet(id: string) {
  const queryClient = useQueryClient();

  return useQuery({
    queryKey: ['pet', id],
    queryFn: () => api.getPet(id),
    refetchInterval: config.pollingInterval,
    enabled: !!id,
    retry: (failureCount, error) => {
      // Don't retry on 404 errors - pet doesn't exist
      if (error instanceof ApiError && error.isNotFound()) {
        // Remove this pet from cache
        queryClient.removeQueries({ queryKey: ['pet', id] });
        return false;
      }
      // Retry server errors up to 3 times
      return failureCount < 3;
    },
  });
}

export function useCreatePet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreatePetRequest) => api.createPet(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pets'] });
    },
  });
}

export function useFeedPet() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (id: string) => api.feedPet(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['pet', id] });
      queryClient.invalidateQueries({ queryKey: ['pets'] });
      queryClient.invalidateQueries({ queryKey: ['progression'] });
    },
    onError: (error: Error, id) => {
      if (error instanceof ApiError && error.isNotFound()) {
        // Pet doesn't exist - remove from cache and notify user
        queryClient.removeQueries({ queryKey: ['pet', id] });
        queryClient.invalidateQueries({ queryKey: ['pets'] });
        toast({
          title: 'Pet not found',
          description: 'This pet no longer exists. Refreshing pet list...',
          variant: 'destructive',
        });
      }
    },
  });
}

export function usePlayWithPet() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (id: string) => api.playWithPet(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['pet', id] });
      queryClient.invalidateQueries({ queryKey: ['pets'] });
      queryClient.invalidateQueries({ queryKey: ['progression'] });
    },
    onError: (error: Error, id) => {
      if (error instanceof ApiError && error.isNotFound()) {
        queryClient.removeQueries({ queryKey: ['pet', id] });
        queryClient.invalidateQueries({ queryKey: ['pets'] });
        toast({
          title: 'Pet not found',
          description: 'This pet no longer exists. Refreshing pet list...',
          variant: 'destructive',
        });
      }
    },
  });
}

export function useCleanPet() {
  const queryClient = useQueryClient();
  const { toast } = useToast();

  return useMutation({
    mutationFn: (id: string) => api.cleanPet(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['pet', id] });
      queryClient.invalidateQueries({ queryKey: ['pets'] });
      queryClient.invalidateQueries({ queryKey: ['progression'] });
    },
    onError: (error: Error, id) => {
      if (error instanceof ApiError && error.isNotFound()) {
        queryClient.removeQueries({ queryKey: ['pet', id] });
        queryClient.invalidateQueries({ queryKey: ['pets'] });
        toast({
          title: 'Pet not found',
          description: 'This pet no longer exists. Refreshing pet list...',
          variant: 'destructive',
        });
      }
    },
  });
}

export function usePetHistory(id: string, limit = 10) {
  return useQuery({
    queryKey: ['pet-history', id, limit],
    queryFn: () => api.getPetHistory(id, limit),
    enabled: !!id,
    refetchInterval: config.pollingInterval,
  });
}
