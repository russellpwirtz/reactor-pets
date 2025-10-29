'use client';

import { usePetHistory } from '@/hooks/use-pets';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { formatDistanceToNow } from 'date-fns';

interface PetHistoryProps {
  petId: string;
}

export function PetHistory({ petId }: PetHistoryProps) {
  const { data: history, isLoading, error } = usePetHistory(petId);

  if (isLoading) return <div>Loading history...</div>;
  if (error) return <div>Error loading history</div>;
  if (!history?.length) {
    return (
      <Card>
        <CardContent className="pt-6">
          <p className="text-muted-foreground text-center">
            No history events yet
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Event History</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {history.map((event, index) => (
            <div
              key={`${event.timestamp}-${index}`}
              className="flex items-start justify-between p-3 border rounded-lg"
            >
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <Badge variant="outline">{event.eventType}</Badge>
                  <span className="text-sm text-muted-foreground">
                    {formatDistanceToNow(new Date(event.timestamp))} ago
                  </span>
                </div>
                <p className="text-sm">{event.payload}</p>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
