'use client';

import React, { useState } from 'react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Maximize2, Minimize2 } from 'lucide-react';
import dynamic from 'next/dynamic';

/**
 * Brain Visualizer Panel Component
 * Wraps the 3D brain visualization with controls and legend
 */

interface BrainVisualizerPanelProps {
  petId: string;
  stage: string;
  isAlive: boolean;
}

// Dynamically import the 3D visualizer to avoid SSR issues with Three.js
const BrainVisualizer3D = dynamic(() => import('./BrainVisualizer3D'), {
  ssr: false,
  loading: () => (
    <div className="w-full h-96 flex items-center justify-center bg-gray-950 rounded">
      <div className="text-gray-400">Loading brain visualization...</div>
    </div>
  ),
});

export default function BrainVisualizerPanel({
  petId,
  stage,
  isAlive,
}: BrainVisualizerPanelProps) {
  const [isFullscreen, setIsFullscreen] = useState(false);

  // Show message for dead pets
  if (!isAlive) {
    return (
      <Card className="p-4">
        <div className="flex justify-between items-center mb-2">
          <div>
            <h3 className="text-lg font-semibold">Brain Activity</h3>
            <p className="text-xs text-gray-500 mt-1">Stage: {stage}</p>
          </div>
        </div>

        <div className="w-full h-96 flex items-center justify-center bg-gray-950 rounded border border-gray-800">
          <div className="text-center text-gray-400">
            <div className="text-6xl mb-4">💀</div>
            <p className="text-lg font-semibold">No Brain Activity</p>
            <p className="text-sm mt-2">This pet has passed away.</p>
            <p className="text-xs mt-1 text-gray-500">
              Brain simulations are only available for living pets.
            </p>
          </div>
        </div>
      </Card>
    );
  }

  return (
    <>
      <Card className="p-4">
        <div className="flex justify-between items-center mb-2">
          <div>
            <h3 className="text-lg font-semibold">Brain Activity</h3>
            <p className="text-xs text-gray-500 mt-1">Stage: {stage}</p>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsFullscreen(!isFullscreen)}
            className="gap-2"
          >
            {isFullscreen ? (
              <>
                <Minimize2 className="h-4 w-4" />
                Minimize
              </>
            ) : (
              <>
                <Maximize2 className="h-4 w-4" />
                Fullscreen
              </>
            )}
          </Button>
        </div>

        <div className={isFullscreen ? '' : 'relative'}>
          <BrainVisualizer3D petId={petId} stage={stage} fullscreen={isFullscreen} />
        </div>

        {/* Legend */}
        <div className="mt-3 text-xs text-gray-500 space-y-1">
          <p className="font-semibold text-gray-400 mb-2">Neural States:</p>
          <div className="flex flex-wrap gap-3">
            <div className="flex items-center gap-2">
              <span className="inline-block w-3 h-3 bg-white border border-gray-300 rounded"></span>
              <span>Firing (Excitatory)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-block w-3 h-3 bg-cyan-400 border border-gray-300 rounded"></span>
              <span>Firing (Inhibitory)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-block w-3 h-3 bg-green-400 border border-gray-300 rounded"></span>
              <span>Active (Excitatory)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-block w-3 h-3 bg-blue-400 border border-gray-300 rounded"></span>
              <span>Active (Inhibitory)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-block w-3 h-3 bg-gray-700 border border-gray-300 rounded"></span>
              <span>Resting</span>
            </div>
          </div>
          <p className="text-gray-600 mt-3 italic">
            Brain activity reflects your pet&apos;s hunger, happiness, and health. Drag to rotate,
            scroll to zoom.
          </p>
        </div>
      </Card>

      {/* Fullscreen overlay */}
      {isFullscreen && (
        <div className="fixed inset-0 z-50 bg-black">
          <div className="absolute top-4 right-4 z-50">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsFullscreen(false)}
              className="gap-2"
            >
              <Minimize2 className="h-4 w-4" />
              Exit Fullscreen
            </Button>
          </div>
          <BrainVisualizer3D petId={petId} stage={stage} fullscreen={true} />
        </div>
      )}
    </>
  );
}
