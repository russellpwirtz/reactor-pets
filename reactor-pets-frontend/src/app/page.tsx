'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatsOverview } from '@/components/stats/stats-overview';
import { StageDistribution } from '@/components/stats/stage-distribution';
import { usePets } from '@/hooks/use-pets';
import { Badge } from '@/components/ui/badge';
import { XPCard } from '@/components/progression/xp-card';
import { AnalyticsCard } from '@/components/progression/analytics-card';
import { useProgression } from '@/hooks/use-progression';
import { PermanentUpgradesCard } from '@/components/progression/permanent-upgrades-card';
import { InventoryCard } from '@/components/equipment/inventory-card';

export default function HomePage() {
  const { data: pets, isLoading } = usePets();
  const { data: progression } = useProgression();

  // Get recent pets (up to 3)
  const recentPets = pets?.slice(0, 3) || [];

  return (
    <div className="container mx-auto py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-4xl font-bold">Dashboard</h1>
        <Link href="/pets">
          <Button>View All Pets</Button>
        </Link>
      </div>

      <div className="grid gap-6">
        {/* Quick Actions */}
        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex gap-4">
              <Link href="/pets">
                <Button variant="outline">Create New Pet</Button>
              </Link>
              <Link href="/leaderboard">
                <Button variant="outline">View Leaderboard</Button>
              </Link>
            </div>
          </CardContent>
        </Card>

        {/* Player Progression */}
        <div className="grid md:grid-cols-2 gap-6">
          {progression && <XPCard {...progression} />}
          <AnalyticsCard />
        </div>

        {/* Permanent Upgrades and Inventory */}
        <div className="grid md:grid-cols-2 gap-6">
          {progression && <PermanentUpgradesCard upgrades={progression.permanentUpgrades} />}
          <InventoryCard />
        </div>

        {/* Global Statistics */}
        <div>
          <h2 className="text-2xl font-semibold mb-4">Global Statistics</h2>
          <StatsOverview />
        </div>

        {/* Stage Distribution */}
        <div className="grid md:grid-cols-2 gap-6">
          <StageDistribution />

          {/* Recent Pets */}
          <Card>
            <CardHeader>
              <CardTitle>Recent Pets</CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div>Loading...</div>
              ) : recentPets.length > 0 ? (
                <div className="space-y-3">
                  {recentPets.map((pet) => (
                    <Link key={pet.petId} href={`/pets/${pet.petId}`}>
                      <div className="flex items-center justify-between p-3 rounded-lg border hover:bg-accent transition-colors">
                        <div>
                          <div className="font-semibold">{pet.name}</div>
                          <div className="text-sm text-muted-foreground">
                            {pet.type} - {pet.stage}
                          </div>
                        </div>
                        <div className="flex gap-2">
                          <Badge variant={pet.alive ? 'default' : 'secondary'}>
                            {pet.alive ? 'Alive' : 'Deceased'}
                          </Badge>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="text-center text-muted-foreground py-4">
                  <p>No pets yet!</p>
                  <Link href="/pets">
                    <Button variant="link" className="mt-2">Create your first pet</Button>
                  </Link>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
