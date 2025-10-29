import { config } from '../config';
import type {
  Pet,
  CreatePetRequest,
  Statistics,
  LeaderboardEntry,
  LeaderboardType,
  PetEvent,
} from '../types';

class ApiError extends Error {
  constructor(
    public status: number,
    public statusText: string,
    public data?: any
  ) {
    super(`API Error: ${status} ${statusText}`);
  }
}

async function fetchApi<T>(
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  const url = `${config.apiBaseUrl}${endpoint}`;

  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new ApiError(response.status, response.statusText, errorData);
  }

  return response.json();
}

export const api = {
  // Pet operations
  createPet: (data: CreatePetRequest) =>
    fetchApi<Pet>('/pets', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getPet: (id: string) => fetchApi<Pet>(`/pets/${id}`),

  getAllPets: () => fetchApi<Pet[]>('/pets'),

  feedPet: (id: string) =>
    fetchApi<Pet>(`/pets/${id}/feed`, { method: 'POST' }),

  playWithPet: (id: string) =>
    fetchApi<Pet>(`/pets/${id}/play`, { method: 'POST' }),

  cleanPet: (id: string) =>
    fetchApi<Pet>(`/pets/${id}/clean`, { method: 'POST' }),

  getPetHistory: (id: string, limit = 10) =>
    fetchApi<PetEvent[]>(`/pets/${id}/history?limit=${limit}`),

  // Statistics
  getStatistics: () => fetchApi<Statistics>('/statistics'),

  getLeaderboard: (type: LeaderboardType = 'AGE') =>
    fetchApi<LeaderboardEntry[]>(`/leaderboard?type=${type}`),
};

export { ApiError };
