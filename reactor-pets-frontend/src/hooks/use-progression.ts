import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { config } from '@/lib/config';

export function useProgression() {
  return useQuery({
    queryKey: ['progression'],
    queryFn: api.getProgression,
    refetchInterval: config.pollingInterval,
  });
}
