'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useStatistics } from '@/hooks/use-statistics';
import { Progress } from '@/components/ui/progress';

export function StageDistribution() {
  const { data: stats } = useStatistics();

  if (!stats) return null;

  const stages = [
    { name: 'Egg', value: stats.stageDistribution.EGG, emoji: '🥚' },
    { name: 'Baby', value: stats.stageDistribution.BABY, emoji: '👶' },
    { name: 'Teen', value: stats.stageDistribution.TEEN, emoji: '🧒' },
    { name: 'Adult', value: stats.stageDistribution.ADULT, emoji: '🦸' },
  ];

  const total = stages.reduce((sum, stage) => sum + stage.value, 0);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Evolution Stage Distribution</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {stages.map((stage) => (
          <div key={stage.name}>
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">
                {stage.emoji} {stage.name}
              </span>
              <span className="text-sm text-muted-foreground">
                {stage.value} ({total > 0 ? ((stage.value / total) * 100).toFixed(0) : 0}%)
              </span>
            </div>
            <Progress value={total > 0 ? (stage.value / total) * 100 : 0} />
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
