'use client';

import { motion } from 'framer-motion';
import { Progress } from '@/components/ui/progress';
import { useEffect, useState } from 'react';

interface AnimatedStatBarProps {
  label: string;
  value: number;
  previousValue?: number;
  inverted?: boolean;
}

export function AnimatedStatBar({
  label,
  value,
  previousValue,
  inverted = false,
}: AnimatedStatBarProps) {
  const [displayValue, setDisplayValue] = useState(previousValue || value);

  useEffect(() => {
    const timer = setTimeout(() => setDisplayValue(value), 100);
    return () => clearTimeout(timer);
  }, [value]);

  const getColor = (v: number) => {
    if (inverted) {
      if (v > 70) return 'text-red-500';
      if (v > 40) return 'text-yellow-500';
      return 'text-green-500';
    }
    if (v < 30) return 'text-red-500';
    if (v < 60) return 'text-yellow-500';
    return 'text-green-500';
  };

  const hasChanged = previousValue !== undefined && previousValue !== value;
  const trend = hasChanged ? (value > previousValue ? '↑' : '↓') : null;

  return (
    <div>
      <div className="flex justify-between text-sm mb-1">
        <span>{label}</span>
        <motion.span
          className={getColor(displayValue)}
          animate={{ scale: hasChanged ? [1, 1.2, 1] : 1 }}
          transition={{ duration: 0.3 }}
        >
          {displayValue}/100 {trend}
        </motion.span>
      </div>
      <Progress value={displayValue} className="h-2" />
    </div>
  );
}
