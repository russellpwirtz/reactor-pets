import { useEffect, useRef } from 'react';
import { toast } from '@/hooks/use-toast';
import { Pet } from '@/lib/types/pet';

export function usePetNotifications(pet: Pet | undefined) {
  const previousStats = useRef<Pet | null>(null);

  useEffect(() => {
    if (!pet || !previousStats.current) {
      previousStats.current = pet || null;
      return;
    }

    const prev = previousStats.current;

    // Critical hunger warning
    if (pet.hunger > 80 && prev.hunger <= 80) {
      toast({
        title: `${pet.name} is very hungry!`,
        description: 'Feed your pet soon',
        variant: 'destructive',
      });
    }

    // Low happiness warning
    if (pet.happiness < 20 && prev.happiness >= 20) {
      toast({
        title: `${pet.name} is sad`,
        description: 'Play with your pet to cheer them up',
      });
    }

    // Low health warning
    if (pet.health < 30 && prev.health >= 30) {
      toast({
        title: `${pet.name} is feeling unwell`,
        description: 'Clean your pet or give medicine',
        variant: 'destructive',
      });
    }

    // Evolution notification
    if (pet.stage !== prev.stage) {
      toast({
        title: `${pet.name} evolved!`,
        description: `Now a ${pet.stage}`,
      });
    }

    // Death notification
    if (!pet.alive && prev.alive) {
      toast({
        title: `${pet.name} has passed away`,
        description: 'Better luck with your next pet',
        variant: 'destructive',
      });
    }

    previousStats.current = pet;
  }, [pet]);
}
