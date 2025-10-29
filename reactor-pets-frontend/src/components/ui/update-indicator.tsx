'use client';

import { useIsFetching } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';

export function UpdateIndicator() {
  const isFetching = useIsFetching();

  if (!isFetching) return null;

  return (
    <div className="fixed bottom-4 right-4 bg-primary text-primary-foreground px-4 py-2 rounded-full shadow-lg flex items-center gap-2 z-50">
      <Loader2 className="h-4 w-4 animate-spin" />
      <span className="text-sm">Updating...</span>
    </div>
  );
}
