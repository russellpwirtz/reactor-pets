'use client';

import { useState } from 'react';
import { PetsList } from '@/components/pets/pets-list';
import { CreatePetDialog } from '@/components/pets/create-pet-dialog';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';

export default function PetsPage() {
  const [aliveOnly, setAliveOnly] = useState(true);

  return (
    <div className="container mx-auto py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-4xl font-bold">My Pets</h1>

        <div className="flex items-center gap-4">
          <div className="flex items-center space-x-2">
            <Switch
              id="alive-filter"
              checked={aliveOnly}
              onCheckedChange={setAliveOnly}
            />
            <Label htmlFor="alive-filter" className="cursor-pointer">
              Alive Only
            </Label>
          </div>
          <CreatePetDialog />
        </div>
      </div>

      <PetsList aliveOnly={aliveOnly} />
    </div>
  );
}
