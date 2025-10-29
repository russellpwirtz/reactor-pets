import { PetsList } from '@/components/pets/pets-list';
import { CreatePetDialog } from '@/components/pets/create-pet-dialog';

export default function PetsPage() {
  return (
    <div className="container mx-auto py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-4xl font-bold">My Pets</h1>
        <CreatePetDialog />
      </div>
      <PetsList />
    </div>
  );
}
