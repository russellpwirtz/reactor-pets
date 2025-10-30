import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { UpgradeType } from '@/lib/types';

interface PermanentUpgradesCardProps {
  upgrades: UpgradeType[];
}

const upgradeLabels: Record<UpgradeType, { name: string; description: string }> = {
  EFFICIENT_METABOLISM: {
    name: 'Efficient Metabolism',
    description: 'Reduces hunger decay rate for all pets'
  },
  HAPPY_DISPOSITION: {
    name: 'Happy Disposition',
    description: 'Reduces happiness decay rate for all pets'
  },
  STURDY_GENETICS: {
    name: 'Sturdy Genetics',
    description: 'Reduces health decay rate for all pets'
  },
  INDUSTRIAL_KITCHEN: {
    name: 'Industrial Kitchen',
    description: 'Increases food effectiveness for all pets'
  },
  FAST_HATCHER: {
    name: 'Fast Hatcher',
    description: 'Reduces hatching time for eggs'
  },
  MULTI_PET_LICENSE_I: {
    name: 'Multi-Pet License I',
    description: 'Increases max pet limit to 2'
  },
  MULTI_PET_LICENSE_II: {
    name: 'Multi-Pet License II',
    description: 'Increases max pet limit to 3'
  },
  MULTI_PET_LICENSE_III: {
    name: 'Multi-Pet License III',
    description: 'Increases max pet limit to 4'
  }
};

export function PermanentUpgradesCard({ upgrades }: PermanentUpgradesCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Permanent Upgrades</CardTitle>
      </CardHeader>
      <CardContent>
        {upgrades.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            No permanent upgrades purchased yet. Visit the shop to purchase upgrades!
          </p>
        ) : (
          <div className="space-y-2">
            {upgrades.map((upgrade) => {
              const info = upgradeLabels[upgrade];
              return (
                <div
                  key={upgrade}
                  className="flex items-start justify-between p-3 rounded-lg border bg-accent/50"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{info.name}</span>
                      <Badge variant="secondary" className="text-xs">
                        Active
                      </Badge>
                    </div>
                    <p className="text-sm text-muted-foreground">{info.description}</p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
