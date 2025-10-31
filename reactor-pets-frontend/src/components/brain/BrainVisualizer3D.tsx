'use client';

import { useEffect, useRef, useState, useMemo } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, PerspectiveCamera, Grid as DreiGrid } from '@react-three/drei';
import * as THREE from 'three';
import { CellState, GridConfig } from './brain-types';

/**
 * 3D Brain Visualization for Pet Neural Activity
 * Uses Server-Sent Events (SSE) to stream brain cell updates
 * Renders neurons with biological color coding:
 * - White/Cyan: Firing neurons
 * - Green/Blue: Active neurons (excitatory/inhibitory)
 * - Dark gray: Resting neurons
 */

interface BrainVisualizer3DProps {
  petId: string;
  stage: string;
  fullscreen?: boolean;
}

// Instanced mesh for efficient rendering of all cells
function InstancedCells({
  gridState,
  gridConfig,
}: {
  gridState: Map<string, CellState>;
  gridConfig: GridConfig;
}) {
  const meshRef = useRef<THREE.InstancedMesh>(null);
  const cellCount = gridConfig.width * gridConfig.height;

  // Create temporary objects for matrix calculations
  const tempObject = useMemo(() => new THREE.Object3D(), []);
  const tempColor = useMemo(() => new THREE.Color(), []);

  // Initialize instanced mesh
  useEffect(() => {
    if (!meshRef.current) return;

    const mesh = meshRef.current;

    // Set initial transforms and colors for all instances
    for (let i = 0; i < cellCount; i++) {
      const x = i % gridConfig.width;
      const y = Math.floor(i / gridConfig.width);

      // Position (center grid around origin)
      const posX = x - gridConfig.width / 2 + 0.5;
      const posZ = y - gridConfig.height / 2 + 0.5;

      tempObject.position.set(posX, 0.05, posZ);
      tempObject.scale.set(0.9, 0.1, 0.9);
      tempObject.updateMatrix();
      mesh.setMatrixAt(i, tempObject.matrix);

      // Initial color (dark gray)
      mesh.setColorAt(i, tempColor.setRGB(0.1, 0.1, 0.1));
    }

    mesh.instanceMatrix.needsUpdate = true;
    if (mesh.instanceColor) mesh.instanceColor.needsUpdate = true;
  }, [cellCount, gridConfig.width, gridConfig.height, tempObject, tempColor]);

  // Update instances based on grid state
  useFrame(() => {
    if (!meshRef.current) return;

    const mesh = meshRef.current;

    // Process all cells in the grid
    for (let i = 0; i < cellCount; i++) {
      const x = i % gridConfig.width;
      const y = Math.floor(i / gridConfig.width);
      const key = `${x}-${y}`;
      const cellState = gridState.get(key);

      if (cellState) {
        // Cell has data - render it normally
        // Backend sends 0-indexed coordinates, center the grid around origin
        const posX = cellState.x - gridConfig.width / 2 + 0.5;
        const posZ = cellState.y - gridConfig.height / 2 + 0.5;

        // Map activation to height (purely data-driven)
        const height = cellState.isFiring ? 2.0 : Math.max(0.1, cellState.activation * 1.5);
        const posY = height / 2;

        tempObject.position.set(posX, posY, posZ);
        tempObject.scale.set(0.9, height, 0.9);
        tempObject.updateMatrix();
        mesh.setMatrixAt(i, tempObject.matrix);

        // Calculate color - Biological coloring
        const isInhibitory = cellState.cellType === 'INHIBITORY';

        // Layer depth affects brightness
        const layerBrightness =
          cellState.layer === 'LAYER_2_3'
            ? 1.0
            : cellState.layer === 'LAYER_4'
              ? 0.9
              : cellState.layer === 'LAYER_5'
                ? 0.75
                : cellState.layer === 'LAYER_6'
                  ? 0.6
                  : 0.8;

        if (cellState.isFiring) {
          // Firing color based on neuron type
          if (isInhibitory) {
            tempColor.setRGB(0.4, 1, 1); // Cyan for inhibitory
          } else {
            tempColor.setRGB(1, 1, 1); // White for excitatory
          }
        } else if (cellState.activation > 0.01) {
          const intensity = Math.abs(cellState.activation);

          if (isInhibitory) {
            // Blue/cyan gradient for inhibitory
            tempColor.setRGB(
              intensity * 0.2 * layerBrightness,
              intensity * 0.6 * layerBrightness,
              intensity * 0.9 * layerBrightness
            );
          } else {
            // Green gradient for excitatory
            tempColor.setRGB(
              intensity * 0.3 * layerBrightness,
              intensity * 0.9 * layerBrightness,
              intensity * 0.1 * layerBrightness
            );
          }
        } else {
          // Inactive - subtle tint based on type
          if (isInhibitory) {
            tempColor.setRGB(0.08, 0.1, 0.12);
          } else {
            tempColor.setRGB(0.1, 0.1, 0.1);
          }
        }

        mesh.setColorAt(i, tempColor);
      } else {
        // No data for this cell - hide it
        tempObject.position.set(0, -1000, 0);
        tempObject.scale.setScalar(0.01);
        tempObject.updateMatrix();
        mesh.setMatrixAt(i, tempObject.matrix);
      }
    }

    mesh.instanceMatrix.needsUpdate = true;
    if (mesh.instanceColor) mesh.instanceColor.needsUpdate = true;
  });

  return (
    <instancedMesh ref={meshRef} args={[undefined, undefined, cellCount]} castShadow receiveShadow>
      <boxGeometry args={[1, 1, 1]} />
      <meshStandardMaterial metalness={0.3} roughness={0.7} />
    </instancedMesh>
  );
}

// Scene component that renders all cells
function GridScene({
  gridState,
  gridConfig,
}: {
  gridState: Map<string, CellState>;
  gridConfig: GridConfig;
}) {
  return (
    <>
      {/* Camera with initial position - scale based on grid size */}
      <PerspectiveCamera
        makeDefault
        position={[gridConfig.width * 0.7, gridConfig.height * 0.5, gridConfig.height * 0.7]}
        fov={50}
      />

      {/* Camera controls - dynamic distance limits based on grid size */}
      <OrbitControls
        enableDamping
        dampingFactor={0.05}
        rotateSpeed={0.5}
        zoomSpeed={0.8}
        minDistance={gridConfig.width * 0.3}
        maxDistance={gridConfig.width * 2}
        maxPolarAngle={Math.PI / 2.1} // Prevent camera from going below ground
      />

      {/* Lighting */}
      <ambientLight intensity={0.3} />
      <directionalLight
        position={[gridConfig.width / 2, 30, gridConfig.height / 2]}
        intensity={0.8}
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
      />
      <pointLight position={[-10, 20, -10]} intensity={0.4} color="#4080ff" />

      {/* Ground grid for reference */}
      <DreiGrid
        args={[gridConfig.width, gridConfig.height]}
        cellSize={1}
        cellThickness={0.5}
        cellColor="#333333"
        sectionSize={5}
        sectionThickness={1}
        sectionColor="#444444"
        fadeDistance={gridConfig.width * 2}
        fadeStrength={1}
        position={[0, -0.01, 0]}
      />

      {/* Ground plane */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.02, 0]} receiveShadow>
        <planeGeometry args={[gridConfig.width * 2, gridConfig.height * 2]} />
        <meshStandardMaterial color="#0a0a0a" />
      </mesh>

      {/* Render all cells using instanced rendering for performance */}
      <InstancedCells gridState={gridState} gridConfig={gridConfig} />
    </>
  );
}

export default function BrainVisualizer3D({
  petId,
  stage,
  fullscreen = false,
}: BrainVisualizer3DProps) {
  const [gridConfig, setGridConfig] = useState<GridConfig>({ width: 20, height: 20 });
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'error'>(
    'connecting'
  );
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [updateCount, setUpdateCount] = useState(0);

  // Store grid state
  const gridStateRef = useRef<Map<string, CellState>>(new Map());
  const [, forceUpdate] = useState({});
  const eventSourceRef = useRef<EventSource | null>(null);
  const gridSizeInitializedRef = useRef(false);

  useEffect(() => {
    // Connect to SSE stream
    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api';
    const eventSource = new EventSource(`${apiBaseUrl}/pets/${petId}/brain/stream`);
    eventSourceRef.current = eventSource;

    let hasConnected = false;
    let messageCount = 0;

    eventSource.onopen = () => {
      console.log('Brain stream connected');
      hasConnected = true;
      setConnectionStatus('connected');
    };

    eventSource.onerror = (error) => {
      console.error('Brain stream error after', messageCount, 'messages:', error);

      // If we never successfully connected, it's likely a backend error (pet not found/dead)
      if (!hasConnected) {
        setConnectionStatus('error');
        setErrorMessage('Unable to connect to brain stream. The pet may not exist, may be dead, or the server is unavailable.');
        eventSource.close();
      } else {
        // Connection was established but lost - EventSource will auto-reconnect
        // Don't close the connection - let it reconnect automatically
        console.log('Brain stream connection lost, EventSource will auto-reconnect...');
        setConnectionStatus('connecting');
      }
    };

    // Handle cell state updates
    eventSource.addEventListener('message', (event) => {
      try {
        messageCount++;
        const cellStates: CellState[] = JSON.parse(event.data);

        // Update grid state
        cellStates.forEach((state) => {
          const key = `${state.x}-${state.y}`;
          gridStateRef.current.set(key, state);
        });

        setUpdateCount((prev) => prev + cellStates.length);

        // Only detect grid size on first message or when we receive a full snapshot (400 cells for 20x20)
        // Buffered updates may contain partial data, so we shouldn't recalculate size from them
        if (!gridSizeInitializedRef.current && cellStates.length > 100) {
          const maxX = Math.max(...cellStates.map((s) => s.x)) + 1;
          const maxY = Math.max(...cellStates.map((s) => s.y)) + 1;

          setGridConfig({ width: maxX, height: maxY });
          gridSizeInitializedRef.current = true;
          console.log(`Grid initialized: ${maxX}x${maxY}`);
        }

        // Throttle re-renders - only update every 50ms
        if (!gridStateRef.current.has('lastRenderTime') ||
            Date.now() - (gridStateRef.current.get('lastRenderTime') as any) > 50) {
          gridStateRef.current.set('lastRenderTime', Date.now() as any);
          forceUpdate({});
        }
      } catch (error) {
        console.error('Failed to parse brain update:', error, 'Data preview:', event.data?.substring(0, 100));
      }
    });

    return () => {
      console.log('Cleaning up brain stream connection');
      eventSource.close();
    };
  }, [petId]);

  const getStatusColor = () => {
    switch (connectionStatus) {
      case 'connected':
        return 'text-green-400';
      case 'connecting':
        return 'text-yellow-400';
      case 'error':
        return 'text-red-400';
    }
  };

  // Show error state if connection failed
  if (connectionStatus === 'error') {
    return (
      <div className={fullscreen ? 'w-full h-full' : 'w-full h-96'}>
        <div className="w-full h-full flex items-center justify-center bg-gray-950 rounded">
          <div className="text-center text-red-400 max-w-md px-4">
            <div className="text-4xl mb-4">⚠️</div>
            <p className="text-lg font-semibold mb-2">Connection Failed</p>
            <p className="text-sm text-gray-400">{errorMessage}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={fullscreen ? 'w-full h-full' : 'w-full h-96'}>
      {/* Status indicator */}
      <div className="absolute top-2 left-2 z-10 bg-black/50 rounded px-3 py-1 text-sm">
        <span className={getStatusColor()}>
          {connectionStatus === 'connected'
            ? '● Connected'
            : connectionStatus === 'connecting'
              ? '○ Connecting...'
              : '✕ Disconnected'}
        </span>
        <span className="text-gray-400 ml-3">
          {gridConfig.width}×{gridConfig.height} | {updateCount} updates
        </span>
      </div>

      {/* 3D Canvas */}
      <Canvas shadows className="bg-gray-950">
        <GridScene gridState={gridStateRef.current} gridConfig={gridConfig} />
      </Canvas>
    </div>
  );
}
