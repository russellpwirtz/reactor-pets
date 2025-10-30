'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Activity, TrendingUp, Zap } from 'lucide-react';
import { useXPAnalytics } from '@/hooks/use-analytics';

/**
 * Phase 7E: XP Analytics Card
 * Displays advanced XP statistics including earning rates and multiplier tracking
 */
export function AnalyticsCard() {
  const { data: analytics, isLoading, error } = useXPAnalytics();

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5" />
            XP Analytics
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </CardContent>
      </Card>
    );
  }

  if (error || !analytics) {
    return null;
  }

  const efficiencyRate = analytics.lifetimeXPEarned > 0
    ? ((analytics.currentXP / analytics.lifetimeXPEarned) * 100).toFixed(1)
    : 0;

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-5 w-5 text-purple-500" />
              XP Analytics
            </CardTitle>
            <CardDescription className="mt-1">
              Phase 7E: Power-leveling metrics
            </CardDescription>
          </div>
          {analytics.highestXPMultiplier >= 5.0 && (
            <Badge variant="destructive" className="gap-1">
              <Zap className="h-3 w-3" />
              MAX (5.0x)
            </Badge>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* XP Earning Rate */}
        <div className="p-4 bg-muted/50 rounded-lg">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium">XP Earning Rate</span>
            <Badge variant="outline">
              {analytics.xpPerMinute > 0
                ? `${analytics.xpPerMinute.toFixed(1)} XP/min`
                : 'Calculating...'}
            </Badge>
          </div>
          <p className="text-xs text-muted-foreground">
            {analytics.xpPerMinute > 0
              ? 'Based on recent earning activity'
              : 'Play for a bit to see your earning rate'}
          </p>
        </div>

        {/* Multiplier Progress */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium flex items-center gap-2">
              <TrendingUp className="h-4 w-4 text-green-500" />
              Highest Multiplier Ever
            </span>
            <span className="text-lg font-bold">
              {analytics.highestXPMultiplier.toFixed(2)}x
            </span>
          </div>
          <div className="w-full bg-muted rounded-full h-2">
            <div
              className="bg-gradient-to-r from-green-500 to-yellow-500 h-2 rounded-full transition-all"
              style={{ width: `${(analytics.highestXPMultiplier / 5.0) * 100}%` }}
            />
          </div>
          <p className="text-xs text-muted-foreground">
            {analytics.highestXPMultiplier >= 5.0
              ? 'You\'ve reached the multiplier cap!'
              : `${((5.0 - analytics.highestXPMultiplier) * 10).toFixed(0)} age milestones to max (5.0x)`}
          </p>
        </div>

        {/* Spending Efficiency */}
        <div className="grid grid-cols-2 gap-4 pt-4 border-t">
          <div>
            <p className="text-xs text-muted-foreground mb-1">Total Earned</p>
            <p className="text-lg font-semibold text-green-600">
              {analytics.lifetimeXPEarned.toLocaleString()}
            </p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground mb-1">Total Spent</p>
            <p className="text-lg font-semibold text-blue-600">
              {analytics.totalXPSpent.toLocaleString()}
            </p>
          </div>
        </div>

        {/* Efficiency Badge */}
        <div className="flex items-center justify-between pt-2 text-xs">
          <span className="text-muted-foreground">Savings Rate</span>
          <Badge variant={Number(efficiencyRate) > 50 ? 'default' : 'secondary'}>
            {efficiencyRate}% unspent
          </Badge>
        </div>
      </CardContent>
    </Card>
  );
}
