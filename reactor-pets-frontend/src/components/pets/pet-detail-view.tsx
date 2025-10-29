'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Button } from '@/components/ui/button';
import { Pet } from '@/lib/types/pet';
import { AsciiArtDisplay } from './ascii-art-display';
import { useFeedPet, usePlayWithPet, useCleanPet } from '@/hooks/use-pets';

interface PetDetailViewProps {
  pet: Pet;
}

export function PetDetailView({ pet }: PetDetailViewProps) {
  const feedPet = useFeedPet();
  const playWithPet = usePlayWithPet();
  const cleanPet = useCleanPet();

  const getStatColor = (value: number, inverted = false) => {
    if (inverted) {
      if (value > 70) return 'text-red-500';
      if (value > 40) return 'text-yellow-500';
      return 'text-green-500';
    }
    if (value < 30) return 'text-red-500';
    if (value < 60) return 'text-yellow-500';
    return 'text-green-500';
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <CardTitle className="text-3xl">{pet.name}</CardTitle>
          <div className="flex gap-2">
            <Badge variant="outline">{pet.type}</Badge>
            <Badge variant="secondary">{pet.stage}</Badge>
            {!pet.alive && <Badge variant="destructive">Deceased</Badge>}
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-6">
        <AsciiArtDisplay art={pet.asciiArt} />

        <div className="grid md:grid-cols-2 gap-6">
          <div className="space-y-4">
            <h3 className="text-lg font-semibold">Stats</h3>
            <div>
              <div className="flex justify-between text-sm mb-1">
                <span>Hunger</span>
                <span className={getStatColor(pet.hunger, true)}>
                  {pet.hunger}/100
                </span>
              </div>
              <Progress value={pet.hunger} className="h-2" />
            </div>

            <div>
              <div className="flex justify-between text-sm mb-1">
                <span>Happiness</span>
                <span className={getStatColor(pet.happiness)}>
                  {pet.happiness}/100
                </span>
              </div>
              <Progress value={pet.happiness} className="h-2" />
            </div>

            <div>
              <div className="flex justify-between text-sm mb-1">
                <span>Health</span>
                <span className={getStatColor(pet.health)}>
                  {pet.health}/100
                </span>
              </div>
              <Progress value={pet.health} className="h-2" />
            </div>
          </div>

          <div className="space-y-2 text-sm">
            <h3 className="text-lg font-semibold mb-4">Information</h3>
            <div><strong>Type:</strong> {pet.type}</div>
            <div><strong>Stage:</strong> {pet.stage}</div>
            <div><strong>Evolution Path:</strong> {pet.evolutionPath}</div>
            <div><strong>Age:</strong> {pet.age} days</div>
            <div><strong>Total Ticks:</strong> {pet.totalTicks}</div>
            <div><strong>Status:</strong> {pet.alive ? 'Alive' : 'Deceased'}</div>
            <div><strong>Last Updated:</strong> {new Date(pet.lastUpdated).toLocaleString()}</div>
          </div>
        </div>

        {pet.alive && (
          <div className="flex gap-2 pt-4">
            <Button
              onClick={() => feedPet.mutate(pet.petId)}
              disabled={feedPet.isPending}
            >
              Feed Pet
            </Button>
            <Button
              variant="outline"
              onClick={() => playWithPet.mutate(pet.petId)}
              disabled={playWithPet.isPending}
            >
              Play with Pet
            </Button>
            <Button
              variant="outline"
              onClick={() => cleanPet.mutate(pet.petId)}
              disabled={cleanPet.isPending}
            >
              Clean Pet
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
