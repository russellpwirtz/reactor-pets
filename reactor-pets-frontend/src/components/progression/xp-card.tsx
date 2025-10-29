'use client';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Coins, TrendingUp, DollarSign } from 'lucide-react';

interface XPCardProps {
  currentXP: number;
  lifetimeXP: number;
  xpMultiplier: number;
  highestMultiplier: number;
  totalXPSpent: number;
}

export function XPCard({
  currentXP,
  lifetimeXP,
  xpMultiplier,
  highestMultiplier,
  totalXPSpent,
}: XPCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Coins className="h-5 w-5 text-yellow-500" />
            Player Progression
          </CardTitle>
          <Badge variant="secondary">
            {xpMultiplier.toFixed(2)}x Multiplier
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-muted-foreground">Current XP</p>
            <p className="text-2xl font-bold">{currentXP.toLocaleString()}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Lifetime XP</p>
            <p className="text-2xl font-bold">{lifetimeXP.toLocaleString()}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 pt-4 border-t">
          <div className="flex items-center gap-2">
            <TrendingUp className="h-4 w-4 text-green-500" />
            <div>
              <p className="text-xs text-muted-foreground">Highest Multiplier</p>
              <p className="text-sm font-medium">{highestMultiplier.toFixed(2)}x</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-blue-500" />
            <div>
              <p className="text-xs text-muted-foreground">Total Spent</p>
              <p className="text-sm font-medium">{totalXPSpent.toLocaleString()}</p>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
