'use client';

import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { useLeaderboard } from '@/hooks/use-statistics';
import type { LeaderboardType } from '@/lib/types/pet';

export default function LeaderboardPage() {
  const [type, setType] = useState<LeaderboardType>('AGE');
  const [aliveOnly, setAliveOnly] = useState(false);
  const { data: entries, isLoading } = useLeaderboard(type, aliveOnly);

  const getRankEmoji = (index: number) => {
    if (index === 0) return '🏆';
    if (index === 1) return '🥈';
    if (index === 2) return '🥉';
    return `${index + 1}.`;
  };

  return (
    <div className="container mx-auto py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-4xl font-bold">Leaderboard</h1>

        <div className="flex items-center space-x-2">
          <Switch
            id="alive-only"
            checked={aliveOnly}
            onCheckedChange={setAliveOnly}
          />
          <Label htmlFor="alive-only" className="cursor-pointer">
            Alive Only
          </Label>
        </div>
      </div>

      <Tabs value={type} onValueChange={(v) => setType(v as LeaderboardType)}>
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="AGE">Oldest</TabsTrigger>
          <TabsTrigger value="HAPPINESS">Happiest</TabsTrigger>
          <TabsTrigger value="HEALTH">Healthiest</TabsTrigger>
        </TabsList>

        <TabsContent value={type} className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Top 10 Pets</CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div>Loading...</div>
              ) : (
                <div className="space-y-2">
                  {entries?.map((entry, index) => (
                    <div
                      key={entry.petId}
                      className="flex items-center justify-between p-3 rounded-lg border"
                    >
                      <div className="flex items-center gap-4">
                        <span className="text-2xl w-8">
                          {getRankEmoji(index)}
                        </span>
                        <div>
                          <div className="font-semibold">{entry.name}</div>
                          <div className="text-sm text-muted-foreground">
                            {entry.type} - {entry.stage}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge variant="outline">{entry.value}</Badge>
                        {!entry.alive && (
                          <Badge variant="secondary">Deceased</Badge>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
