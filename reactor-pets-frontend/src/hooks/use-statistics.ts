import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { config } from '@/lib/config';
import type { LeaderboardType } from '@/lib/types/pet';

export function useStatistics() {
  return useQuery({
    queryKey: ['statistics'],
    queryFn: api.getStatistics,
    refetchInterval: config.pollingInterval,
  });
}

export function useLeaderboard(type: LeaderboardType = 'AGE', aliveOnly: boolean = false) {
  return useQuery({
    queryKey: ['leaderboard', type, aliveOnly],
    queryFn: () => api.getLeaderboard(type, aliveOnly),
    refetchInterval: config.pollingInterval,
  });
}
