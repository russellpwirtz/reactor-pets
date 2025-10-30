import { useQuery } from '@tanstack/react-query';
import { api, ApiError } from '@/lib/api';
import { config } from '@/lib/config';

/**
 * Phase 7E: Hook for XP analytics data
 * Fetches XP earning rates, spending stats, and highest multipliers
 */
export function useXPAnalytics() {
  return useQuery({
    queryKey: ['xp-analytics'],
    queryFn: api.getXPAnalytics,
    refetchInterval: config.pollingInterval,
    retry: (failureCount, error) => {
      // Don't retry on 404 errors - analytics doesn't exist yet
      if (error instanceof ApiError && error.isNotFound()) {
        return false;
      }
      // Retry server errors up to 3 times
      return failureCount < 3;
    },
  });
}
