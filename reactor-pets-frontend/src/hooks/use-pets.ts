import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { config } from '@/lib/config';
import type { CreatePetRequest } from '@/lib/types';

export function usePets() {
  return useQuery({
    queryKey: ['pets'],
    queryFn: api.getAllPets,
    refetchInterval: config.pollingInterval,
  });
}

export function usePet(id: string) {
  return useQuery({
    queryKey: ['pet', id],
    queryFn: () => api.getPet(id),
    refetchInterval: config.pollingInterval,
    enabled: !!id,
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

  return useMutation({
    mutationFn: (id: string) => api.feedPet(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['pet', id] });
      queryClient.invalidateQueries({ queryKey: ['pets'] });
    },
  });
}

export function usePlayWithPet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => api.playWithPet(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['pet', id] });
      queryClient.invalidateQueries({ queryKey: ['pets'] });
    },
  });
}

export function useCleanPet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => api.cleanPet(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['pet', id] });
      queryClient.invalidateQueries({ queryKey: ['pets'] });
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
