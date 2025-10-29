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
    public data?: unknown
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

  getPetHistory: async (id: string, limit = 10) => {
    const response = await fetchApi<{ petId: string; events: PetEvent[]; totalEvents: number }>(`/pets/${id}/history?limit=${limit}`);
    return response.events;
  },

  // Statistics
  getStatistics: () => fetchApi<Statistics>('/statistics'),

  getLeaderboard: async (type: LeaderboardType = 'AGE', aliveOnly: boolean = false) => {
    const response = await fetchApi<{ type: string; pets: Pet[]; totalCount: number }>(
      `/leaderboard?type=${type}&aliveOnly=${aliveOnly}`
    );

    // Map full Pet objects to LeaderboardEntry with the relevant value
    return response.pets.map((pet): LeaderboardEntry => ({
      petId: pet.petId,
      name: pet.name,
      type: pet.type,
      stage: pet.stage,
      alive: pet.alive,
      value: type === 'AGE' ? pet.age : type === 'HAPPINESS' ? pet.happiness : pet.health,
    }));
  },
};

export { ApiError };
