import { useQuery } from '@tanstack/react-query';
import { api, ApiError } from '@/lib/api';
import { config } from '@/lib/config';

export function useProgression() {
  return useQuery({
    queryKey: ['progression'],
    queryFn: api.getProgression,
    refetchInterval: config.pollingInterval,
    retry: (failureCount, error) => {
      // Don't retry on 404 errors - progression doesn't exist yet
      if (error instanceof ApiError && error.isNotFound()) {
        return false;
      }
      // Retry server errors up to 3 times
      return failureCount < 3;
    },
  });
}
