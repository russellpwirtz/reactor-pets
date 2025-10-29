'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Button } from '@/components/ui/button';
import { Pet } from '@/lib/types/pet';
import { useFeedPet, usePlayWithPet, useCleanPet } from '@/hooks/use-pets';
import { formatDistanceToNow } from 'date-fns';
import Link from 'next/link';

interface PetCardProps {
  pet: Pet;
}

export function PetCard({ pet }: PetCardProps) {
  const feedPet = useFeedPet();
  const playWithPet = usePlayWithPet();
  const cleanPet = useCleanPet();

  const getStatColor = (value: number, inverted = false) => {
    if (inverted) {
      // For hunger (high is bad)
      if (value > 70) return 'text-red-500';
      if (value > 40) return 'text-yellow-500';
      return 'text-green-500';
    }
    // For happiness/health (high is good)
    if (value < 30) return 'text-red-500';
    if (value < 60) return 'text-yellow-500';
    return 'text-green-500';
  };

  return (
    <Card className="hover:shadow-lg transition-shadow">
      <CardHeader>
        <div className="flex items-start justify-between">
          <div>
            <CardTitle className="text-xl">{pet.name}</CardTitle>
            <div className="flex gap-2 mt-2">
              <Badge variant="outline">{pet.type}</Badge>
              <Badge variant="secondary">{pet.stage}</Badge>
              {!pet.alive && <Badge variant="destructive">Deceased</Badge>}
            </div>
          </div>
          <Link href={`/pets/${pet.petId}`}>
            <Button variant="ghost" size="sm">View Details</Button>
          </Link>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Stats */}
        <div className="space-y-2">
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

        {/* Info */}
        <div className="text-sm text-muted-foreground space-y-1">
          <div>Age: {pet.age} days</div>
          <div>Evolution: {pet.evolutionPath}</div>
          <div>
            Last updated: {formatDistanceToNow(new Date(pet.lastUpdated))} ago
          </div>
        </div>

        {/* Actions */}
        {pet.alive && (
          <div className="flex gap-2">
            <Button
              size="sm"
              onClick={() => feedPet.mutate(pet.petId)}
              disabled={feedPet.isPending}
            >
              Feed
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => playWithPet.mutate(pet.petId)}
              disabled={playWithPet.isPending}
            >
              Play
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => cleanPet.mutate(pet.petId)}
              disabled={cleanPet.isPending}
            >
              Clean
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
