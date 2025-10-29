'use client';

import { usePets } from '@/hooks/use-pets';
import { PetCard } from './pet-card';
import { PetCardSkeleton } from './pet-card-skeleton';
import { CreatePetDialog } from './create-pet-dialog';

interface PetsListProps {
  aliveOnly?: boolean;
}

export function PetsList({ aliveOnly = false }: PetsListProps) {
  const { data: allPets, isLoading, error } = usePets();

  // Filter pets based on aliveOnly flag
  const pets = aliveOnly ? allPets?.filter(pet => pet.alive) : allPets;

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {[1, 2, 3].map((i) => (
          <PetCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12 text-red-500">
        Error loading pets: {error instanceof Error ? error.message : 'Unknown error'}
      </div>
    );
  }
  if (!pets?.length) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground mb-4">
          You don&apos;t have any pets yet
        </p>
        <CreatePetDialog />
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {pets.map((pet) => (
        <PetCard key={pet.petId} pet={pet} />
      ))}
    </div>
  );
}
