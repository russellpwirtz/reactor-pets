'use client';

import { use } from 'react';
import { usePet } from '@/hooks/use-pets';
import { PetDetailView } from '@/components/pets/pet-detail-view';
import { PetHistory } from '@/components/pets/pet-history';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';

export default function PetDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { data: pet, isLoading } = usePet(id);

  if (isLoading) {
    return (
      <div className="container mx-auto py-8">
        <div>Loading...</div>
      </div>
    );
  }

  if (!pet) {
    return (
      <div className="container mx-auto py-8">
        <div>Pet not found</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8">
      <div className="mb-6">
        <Link href="/pets">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Pets
          </Button>
        </Link>
      </div>

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="history">History</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-6">
          <PetDetailView pet={pet} />
        </TabsContent>

        <TabsContent value="history" className="mt-6">
          <PetHistory petId={pet.petId} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
