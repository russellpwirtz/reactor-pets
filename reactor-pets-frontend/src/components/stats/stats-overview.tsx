'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useStatistics } from '@/hooks/use-statistics';

export function StatsOverview() {
  const { data: stats, isLoading } = useStatistics();

  if (isLoading) return <div>Loading statistics...</div>;
  if (!stats) return null;

  const statCards = [
    {
      title: 'Total Pets Created',
      value: stats.totalPetsCreated,
      icon: '🐾',
    },
    {
      title: 'Currently Alive',
      value: stats.currentlyAlive,
      icon: '❤️',
    },
    {
      title: 'Average Lifespan',
      value: `${stats.averageLifespan.toFixed(1)} days`,
      icon: '⏱️',
    },
    {
      title: 'Longest Lived',
      value: `${stats.longestLivedPetName} (${stats.longestLivedPetAge} days)`,
      icon: '🏆',
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {statCards.map((stat) => (
        <Card key={stat.title}>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">
              {stat.title}
            </CardTitle>
            <span className="text-2xl">{stat.icon}</span>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stat.value}</div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
