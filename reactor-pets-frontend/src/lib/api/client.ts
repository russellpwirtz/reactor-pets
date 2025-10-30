import { config } from '../config';
import type {
  Pet,
  CreatePetRequest,
  Statistics,
  LeaderboardEntry,
  LeaderboardType,
  PetEvent,
  PlayerProgression,
  ShopItem,
  EquippedItems,
  EquipmentInventory,
  EquipmentSlot,
} from '../types';

class ApiError extends Error {
  constructor(
    public status: number,
    public statusText: string,
    public data?: unknown
  ) {
    super(`API Error: ${status} ${statusText}`);
    this.name = 'ApiError';
  }

  isNotFound(): boolean {
    return this.status === 404;
  }

  isServerError(): boolean {
    return this.status >= 500;
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

  // Progression
  getProgression: () => fetchApi<PlayerProgression>('/progression'),

  // Shop
  getShopItems: () => fetchApi<ShopItem[]>('/shop/equipment'),
  getShopUpgrades: () => fetchApi<ShopItem[]>('/shop/upgrades'),
  purchaseEquipment: (equipmentType: string) =>
    fetchApi<void>(`/shop/purchase/equipment/${equipmentType}`, { method: 'POST' }),
  purchaseUpgrade: (upgradeType: string) =>
    fetchApi<void>(`/shop/purchase/upgrade/${upgradeType}`, { method: 'POST' }),
  purchaseConsumable: (consumableType: string) =>
    fetchApi<void>(`/shop/purchase/consumable/${consumableType}`, { method: 'POST' }),

  // Equipment
  getPetEquipment: (petId: string) =>
    fetchApi<EquippedItems>(`/pets/${petId}/equipment`),
  getEquipmentInventory: () =>
    fetchApi<EquipmentInventory>('/inventory/equipment'),
  equipItem: (petId: string, slot: EquipmentSlot, itemId: string) =>
    fetchApi<void>(`/pets/${petId}/equipment/equip`, {
      method: 'POST',
      body: JSON.stringify({ slot, itemId }),
    }),
  unequipItem: (petId: string, slot: EquipmentSlot) =>
    fetchApi<void>(`/pets/${petId}/equipment/unequip`, {
      method: 'POST',
      body: JSON.stringify({ slot }),
    }),
};

export { ApiError };
