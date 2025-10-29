'use client';

import { usePets } from '@/hooks/use-pets';
import { PetCard } from './pet-card';
import { CreatePetDialog } from './create-pet-dialog';

export function PetsList() {
  const { data: pets, isLoading, error } = usePets();

  if (isLoading) return <div>Loading pets...</div>;
  if (error) return <div>Error loading pets</div>;
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
