# Reactor Pets - Frontend Application
## Next.js + TypeScript Implementation Plan

**Prerequisites:** This document assumes the backend REST API (Phase 6) is complete and running. The frontend will consume the API endpoints defined in `06_REST_API.md`.

---

## Overview

This document outlines the implementation plan for a Next.js frontend application that provides a modern, interactive interface for the Reactor Pets virtual pet system. The frontend is designed to be future-ready, with architecture that supports planned features like inventory management, mini-games, LLM chat integration, and RPG progression.

### Technology Stack

- **Framework:** Next.js 15+ (App Router)
- **Language:** TypeScript 5+
- **Node Version:** v22
- **Styling:** Tailwind CSS + shadcn/ui components
- **State Management:** React Context + TanStack Query (React Query)
- **API Client:** Native fetch with TypeScript types
- **Real-time Updates:** Polling (WebSocket support in future phase)
- **UI Components:** shadcn/ui (Radix UI primitives)
- **Animations:** Framer Motion
- **Forms:** React Hook Form + Zod validation

### Project Structure

```
reactor-pets-frontend/
├── src/
│   ├── app/                    # Next.js App Router pages
│   │   ├── layout.tsx         # Root layout
│   │   ├── page.tsx           # Home/Dashboard
│   │   ├── pets/              # Pet management routes
│   │   ├── leaderboard/       # Leaderboard view
│   │   └── api/               # API route handlers (if needed)
│   ├── components/            # React components
│   │   ├── ui/               # shadcn/ui components
│   │   ├── pets/             # Pet-specific components
│   │   ├── stats/            # Statistics components
│   │   └── layout/           # Layout components
│   ├── lib/                  # Utilities and helpers
│   │   ├── api/              # API client
│   │   ├── types/            # TypeScript types
│   │   └── utils/            # Helper functions
│   ├── hooks/                # Custom React hooks
│   └── providers/            # Context providers
├── public/                   # Static assets
├── tests/                    # Test files
└── docs/                     # Documentation
```

---

## Phase 1: Project Foundation & Setup ✅ COMPLETE

**Status:** Completed

**Summary:** Next.js 15 project initialized with TypeScript, Tailwind CSS, and shadcn/ui. API client architecture established with type-safe fetch wrapper, React Query for data management, and complete TypeScript type definitions matching backend DTOs.

**Key Deliverables:**
- Next.js 15 App Router with TypeScript configuration
- React Query provider with 5s polling interval
- Type-safe API client (`src/lib/api/client.ts`) for all backend endpoints
- Complete TypeScript types for Pet, Statistics, and Leaderboard models
- shadcn/ui components: Button, Card, Dialog, Input, Label, Select, Toast, Progress, Badge, Tabs
- Environment configuration with `.env.local` and `src/lib/config.ts`
- Root layout with QueryProvider and Toaster
- All dependencies installed and TypeScript compiling without errors

**Project Location:** `/reactor-pets-frontend/`

---

## Phase 2: Pet Management UI

**Goal:** Implement core pet management features - create, view, and interact with pets.

**Duration Estimate:** Single Claude Code session

### Deliverables

1. **Custom Hooks**
   ```typescript
   // src/hooks/use-pets.ts
   import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
   import { api } from '@/lib/api/client';
   import { config } from '@/lib/config';

   export function usePets() {
     return useQuery({
       queryKey: ['pets'],
       queryFn: api.getAllPets,
       refetchInterval: config.pollingInterval,
     });
   }

   export function usePet(id: string) {
     return useQuery({
       queryKey: ['pet', id],
       queryFn: () => api.getPet(id),
       refetchInterval: config.pollingInterval,
       enabled: !!id,
     });
   }

   export function useCreatePet() {
     const queryClient = useQueryClient();

     return useMutation({
       mutationFn: api.createPet,
       onSuccess: () => {
         queryClient.invalidateQueries({ queryKey: ['pets'] });
       },
     });
   }

   export function useFeedPet() {
     const queryClient = useQueryClient();

     return useMutation({
       mutationFn: (id: string) => api.feedPet(id),
       onSuccess: (_, id) => {
         queryClient.invalidateQueries({ queryKey: ['pet', id] });
         queryClient.invalidateQueries({ queryKey: ['pets'] });
       },
     });
   }

   // Similar hooks for playWithPet, cleanPet
   ```

2. **Create Pet Dialog**
   ```typescript
   // src/components/pets/create-pet-dialog.tsx
   'use client';

   import { useState } from 'react';
   import { useForm } from 'react-hook-form';
   import { zodResolver } from '@hookform/resolvers/zod';
   import * as z from 'zod';
   import {
     Dialog,
     DialogContent,
     DialogHeader,
     DialogTitle,
     DialogTrigger,
   } from '@/components/ui/dialog';
   import { Button } from '@/components/ui/button';
   import { Input } from '@/components/ui/input';
   import { Label } from '@/components/ui/label';
   import {
     Select,
     SelectContent,
     SelectItem,
     SelectTrigger,
     SelectValue,
   } from '@/components/ui/select';
   import { useCreatePet } from '@/hooks/use-pets';
   import { toast } from '@/components/ui/use-toast';

   const createPetSchema = z.object({
     name: z.string().min(1, 'Name is required').max(50),
     type: z.enum(['DOG', 'CAT', 'DRAGON']),
   });

   type CreatePetForm = z.infer<typeof createPetSchema>;

   export function CreatePetDialog() {
     const [open, setOpen] = useState(false);
     const createPet = useCreatePet();

     const form = useForm<CreatePetForm>({
       resolver: zodResolver(createPetSchema),
       defaultValues: { name: '', type: 'CAT' },
     });

     const onSubmit = async (data: CreatePetForm) => {
       try {
         await createPet.mutateAsync(data);
         toast({ title: 'Pet created successfully!' });
         setOpen(false);
         form.reset();
       } catch (error) {
         toast({
           title: 'Failed to create pet',
           description: error.message,
           variant: 'destructive',
         });
       }
     };

     return (
       <Dialog open={open} onOpenChange={setOpen}>
         <DialogTrigger asChild>
           <Button size="lg">Create New Pet</Button>
         </DialogTrigger>
         <DialogContent>
           <DialogHeader>
             <DialogTitle>Create Your Virtual Pet</DialogTitle>
           </DialogHeader>
           <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
             <div>
               <Label htmlFor="name">Pet Name</Label>
               <Input
                 id="name"
                 {...form.register('name')}
                 placeholder="Enter a name..."
               />
               {form.formState.errors.name && (
                 <p className="text-sm text-red-500">
                   {form.formState.errors.name.message}
                 </p>
               )}
             </div>

             <div>
               <Label htmlFor="type">Pet Type</Label>
               <Select
                 onValueChange={(value) => form.setValue('type', value as any)}
                 defaultValue="CAT"
               >
                 <SelectTrigger>
                   <SelectValue />
                 </SelectTrigger>
                 <SelectContent>
                   <SelectItem value="DOG">Dog 🐶</SelectItem>
                   <SelectItem value="CAT">Cat 🐱</SelectItem>
                   <SelectItem value="DRAGON">Dragon 🐉</SelectItem>
                 </SelectContent>
               </Select>
             </div>

             <Button type="submit" className="w-full" disabled={createPet.isPending}>
               {createPet.isPending ? 'Creating...' : 'Create Pet'}
             </Button>
           </form>
         </DialogContent>
       </Dialog>
     );
   }
   ```

3. **Pet Card Component**
   ```typescript
   // src/components/pets/pet-card.tsx
   'use client';

   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Badge } from '@/components/ui/badge';
   import { Progress } from '@/components/ui/progress';
   import { Button } from '@/components/ui/button';
   import { Pet } from '@/lib/types/pet';
   import { useFeedPet, usePlayWithPet, useCleanPet } from '@/hooks/use-pets';
   import { formatDistanceToNow } from 'date-fns';
   import Link from 'next/link';

   interface PetCardProps {
     pet: Pet;
   }

   export function PetCard({ pet }: PetCardProps) {
     const feedPet = useFeedPet();
     const playWithPet = usePlayWithPet();
     const cleanPet = useCleanPet();

     const getStatColor = (value: number, inverted = false) => {
       if (inverted) {
         // For hunger (high is bad)
         if (value > 70) return 'text-red-500';
         if (value > 40) return 'text-yellow-500';
         return 'text-green-500';
       }
       // For happiness/health (high is good)
       if (value < 30) return 'text-red-500';
       if (value < 60) return 'text-yellow-500';
       return 'text-green-500';
     };

     return (
       <Card className="hover:shadow-lg transition-shadow">
         <CardHeader>
           <div className="flex items-start justify-between">
             <div>
               <CardTitle className="text-xl">{pet.name}</CardTitle>
               <div className="flex gap-2 mt-2">
                 <Badge variant="outline">{pet.type}</Badge>
                 <Badge variant="secondary">{pet.stage}</Badge>
                 {!pet.isAlive && <Badge variant="destructive">Deceased</Badge>}
               </div>
             </div>
             <Link href={`/pets/${pet.petId}`}>
               <Button variant="ghost" size="sm">View Details</Button>
             </Link>
           </div>
         </CardHeader>

         <CardContent className="space-y-4">
           {/* Stats */}
           <div className="space-y-2">
             <div>
               <div className="flex justify-between text-sm mb-1">
                 <span>Hunger</span>
                 <span className={getStatColor(pet.hunger, true)}>
                   {pet.hunger}/100
                 </span>
               </div>
               <Progress value={pet.hunger} className="h-2" />
             </div>

             <div>
               <div className="flex justify-between text-sm mb-1">
                 <span>Happiness</span>
                 <span className={getStatColor(pet.happiness)}>
                   {pet.happiness}/100
                 </span>
               </div>
               <Progress value={pet.happiness} className="h-2" />
             </div>

             <div>
               <div className="flex justify-between text-sm mb-1">
                 <span>Health</span>
                 <span className={getStatColor(pet.health)}>
                   {pet.health}/100
                 </span>
               </div>
               <Progress value={pet.health} className="h-2" />
             </div>
           </div>

           {/* Info */}
           <div className="text-sm text-muted-foreground space-y-1">
             <div>Age: {pet.age} days</div>
             <div>Evolution: {pet.evolutionPath}</div>
             <div>
               Last updated: {formatDistanceToNow(new Date(pet.lastUpdated))} ago
             </div>
           </div>

           {/* Actions */}
           {pet.isAlive && (
             <div className="flex gap-2">
               <Button
                 size="sm"
                 onClick={() => feedPet.mutate(pet.petId)}
                 disabled={feedPet.isPending}
               >
                 Feed
               </Button>
               <Button
                 size="sm"
                 variant="outline"
                 onClick={() => playWithPet.mutate(pet.petId)}
                 disabled={playWithPet.isPending}
               >
                 Play
               </Button>
               <Button
                 size="sm"
                 variant="outline"
                 onClick={() => cleanPet.mutate(pet.petId)}
                 disabled={cleanPet.isPending}
               >
                 Clean
               </Button>
             </div>
           )}
         </CardContent>
       </Card>
     );
   }
   ```

4. **Pet List Page**
   ```typescript
   // src/app/pets/page.tsx
   import { PetsList } from '@/components/pets/pets-list';
   import { CreatePetDialog } from '@/components/pets/create-pet-dialog';

   export default function PetsPage() {
     return (
       <div className="container mx-auto py-8">
         <div className="flex items-center justify-between mb-8">
           <h1 className="text-4xl font-bold">My Pets</h1>
           <CreatePetDialog />
         </div>
         <PetsList />
       </div>
     );
   }

   // src/components/pets/pets-list.tsx
   'use client';

   export function PetsList() {
     const { data: pets, isLoading, error } = usePets();

     if (isLoading) return <div>Loading pets...</div>;
     if (error) return <div>Error loading pets</div>;
     if (!pets?.length) {
       return (
         <div className="text-center py-12">
           <p className="text-muted-foreground mb-4">
             You don't have any pets yet
           </p>
           <CreatePetDialog />
         </div>
       );
     }

     return (
       <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
         {pets.map((pet) => (
           <PetCard key={pet.petId} pet={pet} />
         ))}
       </div>
     );
   }
   ```

5. **Pet Detail Page**
   ```typescript
   // src/app/pets/[id]/page.tsx
   'use client';

   import { usePet, usePetHistory } from '@/hooks/use-pets';
   import { PetDetailView } from '@/components/pets/pet-detail-view';
   import { PetHistory } from '@/components/pets/pet-history';
   import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';

   export default function PetDetailPage({ params }: { params: { id: string } }) {
     const { data: pet, isLoading } = usePet(params.id);

     if (isLoading) return <div>Loading...</div>;
     if (!pet) return <div>Pet not found</div>;

     return (
       <div className="container mx-auto py-8">
         <Tabs defaultValue="overview">
           <TabsList>
             <TabsTrigger value="overview">Overview</TabsTrigger>
             <TabsTrigger value="history">History</TabsTrigger>
           </TabsList>

           <TabsContent value="overview">
             <PetDetailView pet={pet} />
           </TabsContent>

           <TabsContent value="history">
             <PetHistory petId={pet.petId} />
           </TabsContent>
         </Tabs>
       </div>
     );
   }
   ```

6. **ASCII Art Display**
   ```typescript
   // src/components/pets/ascii-art-display.tsx
   export function AsciiArtDisplay({ art }: { art: string }) {
     return (
       <pre className="font-mono text-xs bg-muted p-4 rounded-lg overflow-x-auto">
         {art}
       </pre>
     );
   }
   ```

### Testing Checklist

- [ ] Can create new pets via dialog
- [ ] Pet list displays all pets
- [ ] Pet cards show current stats
- [ ] Feed/Play/Clean actions work
- [ ] Navigation to pet detail works
- [ ] Stats update automatically (polling)
- [ ] Error handling displays correctly
- [ ] Loading states display properly

### Technical Notes

- React Query automatically refetches every 5 seconds
- Form validation uses Zod schemas
- shadcn/ui components provide consistent styling
- Progress bars visually indicate stat levels
- Color coding helps identify critical stats

---

## Phase 3: Dashboard & Statistics

**Goal:** Implement global statistics dashboard and leaderboard views.

**Duration Estimate:** Single Claude Code session

### Deliverables

1. **Statistics Hooks**
   ```typescript
   // src/hooks/use-statistics.ts
   export function useStatistics() {
     return useQuery({
       queryKey: ['statistics'],
       queryFn: api.getStatistics,
       refetchInterval: config.pollingInterval,
     });
   }

   export function useLeaderboard(type: LeaderboardType = 'AGE') {
     return useQuery({
       queryKey: ['leaderboard', type],
       queryFn: () => api.getLeaderboard(type),
       refetchInterval: config.pollingInterval,
     });
   }
   ```

2. **Dashboard Page**
   ```typescript
   // src/app/page.tsx (home page)
   import { StatsOverview } from '@/components/stats/stats-overview';
   import { RecentPets } from '@/components/pets/recent-pets';
   import { QuickActions } from '@/components/dashboard/quick-actions';

   export default function HomePage() {
     return (
       <div className="container mx-auto py-8">
         <h1 className="text-4xl font-bold mb-8">Dashboard</h1>

         <div className="grid gap-6">
           <QuickActions />
           <StatsOverview />
           <RecentPets />
         </div>
       </div>
     );
   }
   ```

3. **Stats Overview Component**
   ```typescript
   // src/components/stats/stats-overview.tsx
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
   ```

4. **Stage Distribution Chart**
   ```typescript
   // src/components/stats/stage-distribution.tsx
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
   ```

5. **Leaderboard Page**
   ```typescript
   // src/app/leaderboard/page.tsx
   'use client';

   import { useState } from 'react';
   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
   import { Badge } from '@/components/ui/badge';
   import { useLeaderboard } from '@/hooks/use-statistics';
   import type { LeaderboardType } from '@/lib/types/pet';

   export default function LeaderboardPage() {
     const [type, setType] = useState<LeaderboardType>('AGE');
     const { data: entries, isLoading } = useLeaderboard(type);

     const getRankEmoji = (index: number) => {
       if (index === 0) return '🏆';
       if (index === 1) return '🥈';
       if (index === 2) return '🥉';
       return `${index + 1}.`;
     };

     return (
       <div className="container mx-auto py-8">
         <h1 className="text-4xl font-bold mb-8">Leaderboard</h1>

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
                           {!entry.isAlive && (
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
   ```

6. **Navigation Layout**
   ```typescript
   // src/components/layout/nav-bar.tsx
   'use client';

   import Link from 'next/link';
   import { usePathname } from 'next/navigation';
   import { cn } from '@/lib/utils';

   const navItems = [
     { href: '/', label: 'Dashboard' },
     { href: '/pets', label: 'My Pets' },
     { href: '/leaderboard', label: 'Leaderboard' },
   ];

   export function NavBar() {
     const pathname = usePathname();

     return (
       <nav className="border-b">
         <div className="container mx-auto px-4">
           <div className="flex items-center justify-between h-16">
             <Link href="/" className="text-xl font-bold">
               Reactor Pets 🐾
             </Link>

             <div className="flex gap-6">
               {navItems.map((item) => (
                 <Link
                   key={item.href}
                   href={item.href}
                   className={cn(
                     'text-sm font-medium transition-colors hover:text-primary',
                     pathname === item.href
                       ? 'text-foreground'
                       : 'text-muted-foreground'
                   )}
                 >
                   {item.label}
                 </Link>
               ))}
             </div>
           </div>
         </div>
       </nav>
     );
   }
   ```

### Testing Checklist

- [ ] Dashboard displays global statistics
- [ ] Stage distribution shows percentages correctly
- [ ] Leaderboard shows top 10 pets
- [ ] Can switch between leaderboard types
- [ ] Navigation between pages works
- [ ] Stats update automatically via polling
- [ ] Rank emojis display correctly

### Technical Notes

- Statistics automatically refresh every 5 seconds
- Leaderboard supports three sorting types
- Navigation highlights current page
- Responsive design for mobile/tablet/desktop

---

## Phase 4: Real-time Updates & Polish

**Goal:** Add real-time update indicators, animations, error boundaries, and loading states.

**Duration Estimate:** Single Claude Code session

### Deliverables

1. **Loading Skeletons**
   ```typescript
   // src/components/ui/skeleton.tsx
   // Add via shadcn: npx shadcn-ui@latest add skeleton

   // src/components/pets/pet-card-skeleton.tsx
   import { Card, CardContent, CardHeader } from '@/components/ui/card';
   import { Skeleton } from '@/components/ui/skeleton';

   export function PetCardSkeleton() {
     return (
       <Card>
         <CardHeader>
           <Skeleton className="h-6 w-32" />
           <div className="flex gap-2 mt-2">
             <Skeleton className="h-5 w-16" />
             <Skeleton className="h-5 w-16" />
           </div>
         </CardHeader>
         <CardContent>
           <div className="space-y-4">
             <Skeleton className="h-4 w-full" />
             <Skeleton className="h-4 w-full" />
             <Skeleton className="h-4 w-full" />
           </div>
         </CardContent>
       </Card>
     );
   }
   ```

2. **Error Boundary**
   ```typescript
   // src/components/error-boundary.tsx
   'use client';

   import { Component, ReactNode } from 'react';
   import { Button } from '@/components/ui/button';
   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

   interface Props {
     children: ReactNode;
   }

   interface State {
     hasError: boolean;
     error?: Error;
   }

   export class ErrorBoundary extends Component<Props, State> {
     constructor(props: Props) {
       super(props);
       this.state = { hasError: false };
     }

     static getDerivedStateFromError(error: Error): State {
       return { hasError: true, error };
     }

     render() {
       if (this.state.hasError) {
         return (
           <Card className="max-w-md mx-auto mt-8">
             <CardHeader>
               <CardTitle>Something went wrong</CardTitle>
             </CardHeader>
             <CardContent>
               <p className="text-sm text-muted-foreground mb-4">
                 {this.state.error?.message || 'An unexpected error occurred'}
               </p>
               <Button onClick={() => this.setState({ hasError: false })}>
                 Try Again
               </Button>
             </CardContent>
           </Card>
         );
       }

       return this.props.children;
     }
   }
   ```

3. **Update Indicator**
   ```typescript
   // src/components/ui/update-indicator.tsx
   'use client';

   import { useIsFetching } from '@tanstack/react-query';
   import { Loader2 } from 'lucide-react';

   export function UpdateIndicator() {
     const isFetching = useIsFetching();

     if (!isFetching) return null;

     return (
       <div className="fixed bottom-4 right-4 bg-primary text-primary-foreground px-4 py-2 rounded-full shadow-lg flex items-center gap-2">
         <Loader2 className="h-4 w-4 animate-spin" />
         <span className="text-sm">Updating...</span>
       </div>
     );
   }
   ```

4. **Stat Change Animation**
   ```typescript
   // src/components/pets/animated-stat-bar.tsx
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
   ```

5. **Toast Notifications**
   ```typescript
   // src/hooks/use-pet-notifications.ts
   import { useEffect, useRef } from 'react';
   import { toast } from '@/components/ui/use-toast';
   import { Pet } from '@/lib/types/pet';

   export function usePetNotifications(pet: Pet | undefined) {
     const previousStats = useRef<Pet | null>(null);

     useEffect(() => {
       if (!pet || !previousStats.current) {
         previousStats.current = pet || null;
         return;
       }

       const prev = previousStats.current;

       // Critical hunger warning
       if (pet.hunger > 80 && prev.hunger <= 80) {
         toast({
           title: `${pet.name} is very hungry!`,
           description: 'Feed your pet soon',
           variant: 'destructive',
         });
       }

       // Low happiness warning
       if (pet.happiness < 20 && prev.happiness >= 20) {
         toast({
           title: `${pet.name} is sad`,
           description: 'Play with your pet to cheer them up',
         });
       }

       // Low health warning
       if (pet.health < 30 && prev.health >= 30) {
         toast({
           title: `${pet.name} is feeling unwell`,
           description: 'Clean your pet or give medicine',
           variant: 'destructive',
         });
       }

       // Evolution notification
       if (pet.stage !== prev.stage) {
         toast({
           title: `${pet.name} evolved!`,
           description: `Now a ${pet.stage}`,
         });
       }

       // Death notification
       if (!pet.isAlive && prev.isAlive) {
         toast({
           title: `${pet.name} has passed away`,
           description: 'Better luck with your next pet',
           variant: 'destructive',
         });
       }

       previousStats.current = pet;
     }, [pet]);
   }
   ```

6. **Responsive Layout Updates**
   ```typescript
   // src/app/layout.tsx (updated)
   import { NavBar } from '@/components/layout/nav-bar';
   import { UpdateIndicator } from '@/components/ui/update-indicator';
   import { Toaster } from '@/components/ui/toaster';

   export default function RootLayout({ children }: { children: React.ReactNode }) {
     return (
       <html lang="en">
         <body className={inter.className}>
           <QueryProvider>
             <div className="min-h-screen flex flex-col">
               <NavBar />
               <main className="flex-1">{children}</main>
               <UpdateIndicator />
               <Toaster />
             </div>
           </QueryProvider>
         </body>
       </html>
     );
   }
   ```

7. **Enhanced Pet Detail View**
   ```typescript
   // src/components/pets/pet-detail-view.tsx (updated)
   'use client';

   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { AsciiArtDisplay } from './ascii-art-display';
   import { AnimatedStatBar } from './animated-stat-bar';
   import { usePetNotifications } from '@/hooks/use-pet-notifications';
   import { motion } from 'framer-motion';

   export function PetDetailView({ pet }: { pet: Pet }) {
     usePetNotifications(pet);

     return (
       <motion.div
         initial={{ opacity: 0, y: 20 }}
         animate={{ opacity: 1, y: 0 }}
         transition={{ duration: 0.3 }}
       >
         <Card>
           <CardHeader>
             <CardTitle className="text-3xl">{pet.name}</CardTitle>
           </CardHeader>
           <CardContent className="space-y-6">
             <AsciiArtDisplay art={pet.asciiArt} />

             <div className="grid md:grid-cols-2 gap-6">
               <div className="space-y-4">
                 <AnimatedStatBar label="Hunger" value={pet.hunger} inverted />
                 <AnimatedStatBar label="Happiness" value={pet.happiness} />
                 <AnimatedStatBar label="Health" value={pet.health} />
               </div>

               <div className="space-y-2 text-sm">
                 <div><strong>Type:</strong> {pet.type}</div>
                 <div><strong>Stage:</strong> {pet.stage}</div>
                 <div><strong>Evolution:</strong> {pet.evolutionPath}</div>
                 <div><strong>Age:</strong> {pet.age} days</div>
                 <div><strong>Status:</strong> {pet.isAlive ? 'Alive' : 'Deceased'}</div>
               </div>
             </div>
           </CardContent>
         </Card>
       </motion.div>
     );
   }
   ```

### Testing Checklist

- [ ] Loading skeletons display during data fetch
- [ ] Error boundaries catch and display errors
- [ ] Update indicator appears during background fetches
- [ ] Stat changes animate smoothly
- [ ] Toast notifications trigger on critical stats
- [ ] Evolution notifications appear
- [ ] Death notifications display
- [ ] Responsive design works on all screen sizes

### Technical Notes

- Framer Motion provides smooth animations
- Toast notifications use shadcn/ui toast component
- Update indicator shows when React Query is fetching
- Error boundaries prevent full app crashes
- Responsive design adapts to mobile, tablet, desktop

---

## Phase 5: Items & Inventory UI (Future-Ready)

**Goal:** Create UI components for inventory management, item usage, and preparation for Phase 7 backend (Items System).

**Duration Estimate:** Single Claude Code session

### Deliverables

1. **Inventory Types**
   ```typescript
   // src/lib/types/inventory.ts
   export type ItemType =
     | 'APPLE'
     | 'PIZZA'
     | 'MEDICINE'
     | 'BALL'
     | 'ROBOT'
     // Future items
     | 'HEALTH_POTION'
     | 'MANA_POTION'
     | 'RESPEC_TOKEN'
     | 'SKILL_BOOK'
     | 'ENCHANTED_COLLAR'
     | 'IRON_ARMOR'
     | 'SPEED_BOOTS';

   export interface InventoryItem {
     itemType: ItemType;
     quantity: number;
     description: string;
     rarity?: 'COMMON' | 'UNCOMMON' | 'RARE' | 'LEGENDARY';
   }

   export interface Inventory {
     inventoryId: string;
     petId: string;
     items: InventoryItem[];
     lastUpdated: string;
   }
   ```

2. **Inventory Component**
   ```typescript
   // src/components/inventory/inventory-grid.tsx
   'use client';

   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Badge } from '@/components/ui/badge';
   import { Button } from '@/components/ui/button';
   import { InventoryItem } from '@/lib/types/inventory';

   interface InventoryGridProps {
     items: InventoryItem[];
     onUseItem: (itemType: string) => void;
   }

   const itemEmojis: Record<string, string> = {
     APPLE: '🍎',
     PIZZA: '🍕',
     MEDICINE: '💊',
     BALL: '⚽',
     ROBOT: '🤖',
     HEALTH_POTION: '🧪',
     MANA_POTION: '🔮',
     RESPEC_TOKEN: '📜',
     SKILL_BOOK: '📚',
     ENCHANTED_COLLAR: '📿',
     IRON_ARMOR: '🛡️',
     SPEED_BOOTS: '👢',
   };

   export function InventoryGrid({ items, onUseItem }: InventoryGridProps) {
     return (
       <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
         {items.map((item) => (
           <Card key={item.itemType} className="hover:shadow-md transition-shadow">
             <CardHeader className="pb-3">
               <div className="text-center text-4xl mb-2">
                 {itemEmojis[item.itemType] || '📦'}
               </div>
               <CardTitle className="text-sm text-center">
                 {item.itemType.replace('_', ' ')}
               </CardTitle>
             </CardHeader>
             <CardContent className="space-y-2">
               <div className="flex items-center justify-between">
                 <Badge variant="outline">x{item.quantity}</Badge>
                 {item.rarity && (
                   <Badge
                     variant={
                       item.rarity === 'LEGENDARY'
                         ? 'default'
                         : item.rarity === 'RARE'
                         ? 'secondary'
                         : 'outline'
                     }
                   >
                     {item.rarity}
                   </Badge>
                 )}
               </div>
               <p className="text-xs text-muted-foreground">
                 {item.description}
               </p>
               <Button
                 size="sm"
                 className="w-full"
                 onClick={() => onUseItem(item.itemType)}
                 disabled={item.quantity === 0}
               >
                 Use
               </Button>
             </CardContent>
           </Card>
         ))}
       </div>
     );
   }
   ```

3. **Inventory Page**
   ```typescript
   // src/app/inventory/page.tsx
   'use client';

   import { InventoryGrid } from '@/components/inventory/inventory-grid';
   import { Button } from '@/components/ui/button';
   import { Alert, AlertDescription } from '@/components/ui/alert';

   export default function InventoryPage() {
     // Mock data until backend Phase 7 is complete
     const mockInventory = [
       {
         itemType: 'APPLE' as const,
         quantity: 10,
         description: 'Reduces hunger by 15',
         rarity: 'COMMON' as const,
       },
       {
         itemType: 'BALL' as const,
         quantity: 5,
         description: 'Increases happiness by 10',
         rarity: 'COMMON' as const,
       },
       {
         itemType: 'MEDICINE' as const,
         quantity: 2,
         description: 'Restores health by 20',
         rarity: 'UNCOMMON' as const,
       },
     ];

     const handleUseItem = (itemType: string) => {
       console.log('Using item:', itemType);
       // TODO: Implement when backend Phase 7 is complete
       alert('Item system coming in Phase 7!');
     };

     return (
       <div className="container mx-auto py-8">
         <div className="flex items-center justify-between mb-8">
           <h1 className="text-4xl font-bold">Inventory</h1>
           <Button variant="outline">Shop (Coming Soon)</Button>
         </div>

         <Alert className="mb-6">
           <AlertDescription>
             Full inventory system will be available after backend Phase 7 is
             implemented. This is a preview of the UI.
           </AlertDescription>
         </Alert>

         <InventoryGrid items={mockInventory} onUseItem={handleUseItem} />
       </div>
     );
   }
   ```

4. **Item Usage Dialog**
   ```typescript
   // src/components/inventory/use-item-dialog.tsx
   'use client';

   import {
     Dialog,
     DialogContent,
     DialogHeader,
     DialogTitle,
   } from '@/components/ui/dialog';
   import { Button } from '@/components/ui/button';
   import { InventoryItem } from '@/lib/types/inventory';

   interface UseItemDialogProps {
     item: InventoryItem | null;
     open: boolean;
     onClose: () => void;
     onConfirm: () => void;
   }

   export function UseItemDialog({
     item,
     open,
     onClose,
     onConfirm,
   }: UseItemDialogProps) {
     if (!item) return null;

     return (
       <Dialog open={open} onOpenChange={onClose}>
         <DialogContent>
           <DialogHeader>
             <DialogTitle>Use {item.itemType.replace('_', ' ')}?</DialogTitle>
           </DialogHeader>
           <div className="space-y-4">
             <p className="text-sm text-muted-foreground">{item.description}</p>
             <div className="flex gap-2">
               <Button onClick={onConfirm} className="flex-1">
                 Use Item
               </Button>
               <Button onClick={onClose} variant="outline" className="flex-1">
                 Cancel
               </Button>
             </div>
           </div>
         </DialogContent>
       </Dialog>
     );
   }
   ```

### Testing Checklist

- [ ] Inventory grid displays items correctly
- [ ] Item quantities show correctly
- [ ] Rarity badges display with correct styling
- [ ] Use item button is disabled when quantity is 0
- [ ] Use item dialog appears on button click
- [ ] Alert banner explains Phase 7 requirement

### Technical Notes

- UI components ready for backend integration
- Mock data used until backend Phase 7 complete
- Item emojis provide visual identification
- Rarity system styled consistently
- Dialog confirms item usage action

---

## Phase 6: Mini-Games & Achievements UI

**Goal:** Create interactive mini-game interfaces and achievement display, preparing for Phase 8 backend.

**Duration Estimate:** Single Claude Code session

### Deliverables

1. **Achievement Types**
   ```typescript
   // src/lib/types/achievements.ts
   export interface Achievement {
     id: string;
     name: string;
     description: string;
     category: 'CARE' | 'SURVIVAL' | 'EVOLUTION' | 'SOCIAL' | 'GAME';
     icon: string;
     unlocked: boolean;
     unlockedAt?: string;
     progress?: number;
     maxProgress?: number;
   }

   export interface AchievementCategory {
     category: string;
     achievements: Achievement[];
   }
   ```

2. **Achievements Page**
   ```typescript
   // src/app/achievements/page.tsx
   'use client';

   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Badge } from '@/components/ui/badge';
   import { Progress } from '@/components/ui/progress';
   import { Lock, Check } from 'lucide-react';

   export default function AchievementsPage() {
     // Mock data until backend Phase 8
     const mockAchievements = [
       {
         id: 'first-pet',
         name: 'First Pet',
         description: 'Create your first pet',
         category: 'CARE' as const,
         icon: '🐾',
         unlocked: true,
         unlockedAt: '2025-10-28T10:00:00Z',
       },
       {
         id: 'veteran-owner',
         name: 'Veteran Owner',
         description: 'Create 10 pets',
         category: 'CARE' as const,
         icon: '👑',
         unlocked: false,
         progress: 3,
         maxProgress: 10,
       },
       {
         id: 'perfect-care',
         name: 'Perfect Care',
         description: 'Raise a pet to adult with all stats above 90',
         category: 'EVOLUTION' as const,
         icon: '⭐',
         unlocked: false,
       },
       {
         id: 'survivor',
         name: 'Survivor',
         description: 'Keep a pet alive for 500 ticks',
         category: 'SURVIVAL' as const,
         icon: '🏆',
         unlocked: false,
         progress: 125,
         maxProgress: 500,
       },
     ];

     const groupedAchievements = mockAchievements.reduce((acc, achievement) => {
       if (!acc[achievement.category]) {
         acc[achievement.category] = [];
       }
       acc[achievement.category].push(achievement);
       return acc;
     }, {} as Record<string, typeof mockAchievements>);

     return (
       <div className="container mx-auto py-8">
         <h1 className="text-4xl font-bold mb-8">Achievements</h1>

         <div className="space-y-6">
           {Object.entries(groupedAchievements).map(([category, achievements]) => (
             <div key={category}>
               <h2 className="text-2xl font-semibold mb-4">{category}</h2>
               <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                 {achievements.map((achievement) => (
                   <Card
                     key={achievement.id}
                     className={achievement.unlocked ? 'border-primary' : ''}
                   >
                     <CardHeader>
                       <div className="flex items-start justify-between">
                         <div className="flex items-center gap-2">
                           <span className="text-3xl">{achievement.icon}</span>
                           <div>
                             <CardTitle className="text-lg">
                               {achievement.name}
                             </CardTitle>
                           </div>
                         </div>
                         {achievement.unlocked ? (
                           <Check className="h-5 w-5 text-primary" />
                         ) : (
                           <Lock className="h-5 w-5 text-muted-foreground" />
                         )}
                       </div>
                     </CardHeader>
                     <CardContent className="space-y-2">
                       <p className="text-sm text-muted-foreground">
                         {achievement.description}
                       </p>

                       {achievement.unlocked && achievement.unlockedAt && (
                         <Badge variant="secondary">
                           Unlocked {new Date(achievement.unlockedAt).toLocaleDateString()}
                         </Badge>
                       )}

                       {!achievement.unlocked &&
                         achievement.progress !== undefined &&
                         achievement.maxProgress && (
                           <div>
                             <div className="flex justify-between text-xs text-muted-foreground mb-1">
                               <span>Progress</span>
                               <span>
                                 {achievement.progress}/{achievement.maxProgress}
                               </span>
                             </div>
                             <Progress
                               value={(achievement.progress / achievement.maxProgress) * 100}
                             />
                           </div>
                         )}
                     </CardContent>
                   </Card>
                 ))}
               </div>
             </div>
           ))}
         </div>
       </div>
     );
   }
   ```

3. **Mini-Game Components**
   ```typescript
   // src/components/games/game-card.tsx
   'use client';

   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Button } from '@/components/ui/button';
   import { Badge } from '@/components/ui/badge';

   interface GameCardProps {
     title: string;
     description: string;
     xpReward: number;
     cooldown?: number;
     icon: string;
     onPlay: () => void;
     disabled?: boolean;
   }

   export function GameCard({
     title,
     description,
     xpReward,
     cooldown,
     icon,
     onPlay,
     disabled,
   }: GameCardProps) {
     return (
       <Card>
         <CardHeader>
           <div className="flex items-center gap-3">
             <span className="text-4xl">{icon}</span>
             <div>
               <CardTitle>{title}</CardTitle>
               <Badge variant="secondary" className="mt-1">
                 +{xpReward} XP
               </Badge>
             </div>
           </div>
         </CardHeader>
         <CardContent className="space-y-4">
           <p className="text-sm text-muted-foreground">{description}</p>

           {cooldown && cooldown > 0 && (
             <p className="text-sm text-yellow-500">
               Available in {cooldown} minutes
             </p>
           )}

           <Button onClick={onPlay} disabled={disabled || (cooldown && cooldown > 0)} className="w-full">
             Play Now
           </Button>
         </CardContent>
       </Card>
     );
   }
   ```

4. **Games Page**
   ```typescript
   // src/app/games/page.tsx
   'use client';

   import { GameCard } from '@/components/games/game-card';
   import { Alert, AlertDescription } from '@/components/ui/alert';

   export default function GamesPage() {
     const games = [
       {
         id: 'guess',
         title: 'Guess Game',
         description: 'Guess a number between 1-5. Success increases happiness!',
         xpReward: 30,
         icon: '🎲',
       },
       {
         id: 'reflex',
         title: 'Reflex Game',
         description: 'Test your reaction time. Fast reactions earn more happiness!',
         xpReward: 30,
         icon: '⚡',
       },
       {
         id: 'emotional',
         title: 'Emotional Intelligence',
         description: 'Interpret your pet\'s needs through subtle hints',
         xpReward: 30,
         icon: '🧠',
       },
     ];

     const handlePlayGame = (gameId: string) => {
       alert(`${gameId} game coming in Phase 8!`);
     };

     return (
       <div className="container mx-auto py-8">
         <h1 className="text-4xl font-bold mb-8">Mini-Games</h1>

         <Alert className="mb-6">
           <AlertDescription>
             Mini-games will be fully functional after backend Phase 8. This is a
             preview of the UI.
           </AlertDescription>
         </Alert>

         <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
           {games.map((game) => (
             <GameCard
               key={game.id}
               title={game.title}
               description={game.description}
               xpReward={game.xpReward}
               icon={game.icon}
               onPlay={() => handlePlayGame(game.id)}
             />
           ))}
         </div>
       </div>
     );
   }
   ```

5. **Update Navigation**
   ```typescript
   // src/components/layout/nav-bar.tsx (updated)
   const navItems = [
     { href: '/', label: 'Dashboard' },
     { href: '/pets', label: 'My Pets' },
     { href: '/inventory', label: 'Inventory' },
     { href: '/games', label: 'Games' },
     { href: '/achievements', label: 'Achievements' },
     { href: '/leaderboard', label: 'Leaderboard' },
   ];
   ```

### Testing Checklist

- [ ] Achievements page displays categories correctly
- [ ] Locked/unlocked achievements styled differently
- [ ] Progress bars show for incomplete achievements
- [ ] Games page displays mini-game cards
- [ ] XP rewards displayed correctly
- [ ] Navigation includes new pages
- [ ] Alert banners explain Phase 8 requirement

### Technical Notes

- Achievement progress tracked with progress bars
- Games show XP rewards and cooldowns
- Icons provide visual identification
- Mock data until backend Phase 8
- UI ready for backend integration

---

## Phase 7: Chat Interface (LLM-Ready)

**Goal:** Create chat interface for LLM integration, preparing for Phases 10-14 backend (Pet Personality & Chat).

**Duration Estimate:** Single Claude Code session

### Deliverables

1. **Chat Types**
   ```typescript
   // src/lib/types/chat.ts
   export interface ChatMessage {
     id: string;
     sender: 'USER' | 'PET';
     message: string;
     timestamp: string;
   }

   export interface PetPersonality {
     petId: string;
     traits: string[];
     mood: string;
     moodStability: number;
   }
   ```

2. **Chat Component**
   ```typescript
   // src/components/chat/chat-interface.tsx
   'use client';

   import { useState, useRef, useEffect } from 'react';
   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Button } from '@/components/ui/button';
   import { Input } from '@/components/ui/input';
   import { Badge } from '@/components/ui/badge';
   import { Send } from 'lucide-react';
   import { ChatMessage } from '@/lib/types/chat';

   interface ChatInterfaceProps {
     petName: string;
     petId: string;
     personality?: string[];
     mood?: string;
   }

   export function ChatInterface({
     petName,
     petId,
     personality = ['Friendly', 'Playful'],
     mood = 'Happy',
   }: ChatInterfaceProps) {
     const [messages, setMessages] = useState<ChatMessage[]>([
       {
         id: '1',
         sender: 'PET',
         message: `Hi! I'm ${petName}. How are you today?`,
         timestamp: new Date().toISOString(),
       },
     ]);
     const [input, setInput] = useState('');
     const messagesEndRef = useRef<HTMLDivElement>(null);

     const scrollToBottom = () => {
       messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
     };

     useEffect(() => {
       scrollToBottom();
     }, [messages]);

     const handleSend = () => {
       if (!input.trim()) return;

       // Add user message
       const userMessage: ChatMessage = {
         id: Date.now().toString(),
         sender: 'USER',
         message: input,
         timestamp: new Date().toISOString(),
       };
       setMessages((prev) => [...prev, userMessage]);

       // Mock pet response (replace with API call in Phase 11)
       setTimeout(() => {
         const petMessage: ChatMessage = {
           id: (Date.now() + 1).toString(),
           sender: 'PET',
           message:
             'LLM chat will be available after backend Phase 11. For now, this is just a UI preview!',
           timestamp: new Date().toISOString(),
         };
         setMessages((prev) => [...prev, petMessage]);
       }, 500);

       setInput('');
     };

     return (
       <Card className="h-[600px] flex flex-col">
         <CardHeader>
           <div className="flex items-center justify-between">
             <CardTitle>Chat with {petName}</CardTitle>
             <div className="flex gap-2">
               {personality.map((trait) => (
                 <Badge key={trait} variant="secondary">
                   {trait}
                 </Badge>
               ))}
               <Badge variant="outline">{mood}</Badge>
             </div>
           </div>
         </CardHeader>

         <CardContent className="flex-1 flex flex-col">
           <div className="flex-1 overflow-y-auto space-y-4 mb-4">
             {messages.map((msg) => (
               <div
                 key={msg.id}
                 className={`flex ${msg.sender === 'USER' ? 'justify-end' : 'justify-start'}`}
               >
                 <div
                   className={`max-w-[70%] rounded-lg p-3 ${
                     msg.sender === 'USER'
                       ? 'bg-primary text-primary-foreground'
                       : 'bg-muted'
                   }`}
                 >
                   <p className="text-sm">{msg.message}</p>
                   <p className="text-xs opacity-70 mt-1">
                     {new Date(msg.timestamp).toLocaleTimeString()}
                   </p>
                 </div>
               </div>
             ))}
             <div ref={messagesEndRef} />
           </div>

           <div className="flex gap-2">
             <Input
               value={input}
               onChange={(e) => setInput(e.target.value)}
               onKeyPress={(e) => e.key === 'Enter' && handleSend()}
               placeholder="Type a message..."
             />
             <Button onClick={handleSend} size="icon">
               <Send className="h-4 w-4" />
             </Button>
           </div>
         </CardContent>
       </Card>
     );
   }
   ```

3. **Add Chat to Pet Detail**
   ```typescript
   // src/app/pets/[id]/page.tsx (updated)
   import { ChatInterface } from '@/components/chat/chat-interface';

   export default function PetDetailPage({ params }: { params: { id: string } }) {
     const { data: pet } = usePet(params.id);

     if (!pet) return <div>Loading...</div>;

     return (
       <div className="container mx-auto py-8">
         <Tabs defaultValue="overview">
           <TabsList>
             <TabsTrigger value="overview">Overview</TabsTrigger>
             <TabsTrigger value="chat">Chat</TabsTrigger>
             <TabsTrigger value="history">History</TabsTrigger>
           </TabsList>

           <TabsContent value="overview">
             <PetDetailView pet={pet} />
           </TabsContent>

           <TabsContent value="chat">
             <ChatInterface petName={pet.name} petId={pet.petId} />
           </TabsContent>

           <TabsContent value="history">
             <PetHistory petId={pet.petId} />
           </TabsContent>
         </Tabs>
       </div>
     );
   }
   ```

4. **Personality Display**
   ```typescript
   // src/components/pets/personality-card.tsx
   'use client';

   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Badge } from '@/components/ui/badge';
   import { Progress } from '@/components/ui/progress';

   interface PersonalityCardProps {
     traits: string[];
     mood: string;
     moodStability: number;
   }

   export function PersonalityCard({
     traits,
     mood,
     moodStability,
   }: PersonalityCardProps) {
     return (
       <Card>
         <CardHeader>
           <CardTitle>Personality</CardTitle>
         </CardHeader>
         <CardContent className="space-y-4">
           <div>
             <p className="text-sm font-medium mb-2">Traits</p>
             <div className="flex flex-wrap gap-2">
               {traits.map((trait) => (
                 <Badge key={trait} variant="secondary">
                   {trait}
                 </Badge>
               ))}
             </div>
           </div>

           <div>
             <p className="text-sm font-medium mb-2">Current Mood: {mood}</p>
             <div className="space-y-1">
               <div className="flex justify-between text-xs">
                 <span>Mood Stability</span>
                 <span>{moodStability}/100</span>
               </div>
               <Progress value={moodStability} />
             </div>
           </div>
         </CardContent>
       </Card>
     );
   }
   ```

### Testing Checklist

- [ ] Chat interface displays messages correctly
- [ ] Can send messages via input
- [ ] Messages scroll automatically
- [ ] User/pet messages styled differently
- [ ] Personality traits display
- [ ] Mood indicator shows
- [ ] Enter key sends message
- [ ] Chat tab appears in pet detail

### Technical Notes

- Chat UI ready for LLM integration
- Mock responses until backend Phase 11
- Auto-scrolling for new messages
- Personality traits displayed as badges
- Mood stability shown with progress bar

---

## Phase 8: RPG & Combat UI (Future-Ready)

**Goal:** Create UI for XP, levels, skill trees, and dungeon combat, preparing for Phases 15-19 backend (RPG Progression).

**Duration Estimate:** Single Claude Code session

### Deliverables

1. **RPG Types**
   ```typescript
   // src/lib/types/rpg.ts
   export interface Progression {
     petId: string;
     level: number;
     currentXP: number;
     totalXP: number;
     xpForNextLevel: number;
     unspentSkillPoints: number;
     allocatedSkills: Record<string, number>;
     primaryTree: 'FIRE' | 'SCALES' | 'AGILITY' | null;
   }

   export interface Skill {
     id: string;
     name: string;
     description: string;
     tree: 'FIRE' | 'SCALES' | 'AGILITY';
     currentRank: number;
     maxRank: number;
     levelRequired: number;
     prerequisite: string | null;
     icon: string;
   }

   export interface DungeonRun {
     runId: string;
     petId: string;
     currentFloor: number;
     petCurrentHP: number;
     petMaxHP: number;
     enemies: Enemy[];
     state: 'IN_PROGRESS' | 'VICTORY' | 'RETREATED' | 'DEFEATED';
   }

   export interface Enemy {
     id: string;
     type: string;
     currentHP: number;
     maxHP: number;
     attack: number;
     defense: number;
   }
   ```

2. **XP & Level Display**
   ```typescript
   // src/components/rpg/level-card.tsx
   'use client';

   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Badge } from '@/components/ui/badge';
   import { Progress } from '@/components/ui/progress';
   import { Zap } from 'lucide-react';

   interface LevelCardProps {
     level: number;
     currentXP: number;
     xpForNextLevel: number;
     totalXP: number;
     unspentSkillPoints: number;
   }

   export function LevelCard({
     level,
     currentXP,
     xpForNextLevel,
     totalXP,
     unspentSkillPoints,
   }: LevelCardProps) {
     const progress = (currentXP / xpForNextLevel) * 100;

     return (
       <Card>
         <CardHeader>
           <div className="flex items-center justify-between">
             <CardTitle className="flex items-center gap-2">
               <Zap className="h-5 w-5 text-yellow-500" />
               Level {level}
             </CardTitle>
             {unspentSkillPoints > 0 && (
               <Badge variant="destructive">
                 {unspentSkillPoints} Skill Point{unspentSkillPoints > 1 ? 's' : ''}
               </Badge>
             )}
           </div>
         </CardHeader>
         <CardContent className="space-y-4">
           <div>
             <div className="flex justify-between text-sm mb-2">
               <span>XP Progress</span>
               <span className="font-medium">
                 {currentXP} / {xpForNextLevel}
               </span>
             </div>
             <Progress value={progress} className="h-3" />
             <p className="text-xs text-muted-foreground mt-1">
               {xpForNextLevel - currentXP} XP to next level
             </p>
           </div>

           <div className="text-sm text-muted-foreground">
             Total XP Earned: {totalXP.toLocaleString()}
           </div>
         </CardContent>
       </Card>
     );
   }
   ```

3. **Skill Tree Visualization**
   ```typescript
   // src/components/rpg/skill-tree.tsx
   'use client';

   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Button } from '@/components/ui/button';
   import { Badge } from '@/components/ui/badge';
   import { Lock, Check } from 'lucide-react';
   import { Skill } from '@/lib/types/rpg';

   interface SkillTreeProps {
     tree: 'FIRE' | 'SCALES' | 'AGILITY';
     skills: Skill[];
     unspentPoints: number;
     onAllocate: (skillId: string) => void;
   }

   const treeInfo = {
     FIRE: { name: 'Fire Tree', icon: '🔥', description: 'Offensive power' },
     SCALES: { name: 'Scales Tree', icon: '🛡️', description: 'Defensive tank' },
     AGILITY: { name: 'Agility Tree', icon: '⚡', description: 'Speed & utility' },
   };

   export function SkillTree({ tree, skills, unspentPoints, onAllocate }: SkillTreeProps) {
     const info = treeInfo[tree];

     return (
       <Card>
         <CardHeader>
           <div className="flex items-center gap-2">
             <span className="text-2xl">{info.icon}</span>
             <div>
               <CardTitle>{info.name}</CardTitle>
               <p className="text-sm text-muted-foreground">{info.description}</p>
             </div>
           </div>
         </CardHeader>
         <CardContent className="space-y-3">
           {skills.map((skill) => {
             const isMaxed = skill.currentRank >= skill.maxRank;
             const isLocked = skill.prerequisite && !skill.currentRank;
             const canAllocate = !isMaxed && !isLocked && unspentPoints > 0;

             return (
               <div
                 key={skill.id}
                 className="flex items-center justify-between p-3 border rounded-lg"
               >
                 <div className="flex-1">
                   <div className="flex items-center gap-2 mb-1">
                     <span className="font-medium">{skill.name}</span>
                     {isMaxed && <Check className="h-4 w-4 text-green-500" />}
                     {isLocked && <Lock className="h-4 w-4 text-muted-foreground" />}
                   </div>
                   <p className="text-xs text-muted-foreground">
                     {skill.description}
                   </p>
                   <div className="flex gap-2 mt-2">
                     <Badge variant="outline" className="text-xs">
                       {skill.currentRank}/{skill.maxRank}
                     </Badge>
                     <Badge variant="secondary" className="text-xs">
                       Lvl {skill.levelRequired}
                     </Badge>
                   </div>
                 </div>

                 <Button
                   size="sm"
                   onClick={() => onAllocate(skill.id)}
                   disabled={!canAllocate}
                 >
                   Allocate
                 </Button>
               </div>
             );
           })}
         </CardContent>
       </Card>
     );
   }
   ```

4. **Skills Page**
   ```typescript
   // src/app/skills/page.tsx
   'use client';

   import { useState } from 'react';
   import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
   import { Alert, AlertDescription } from '@/components/ui/alert';
   import { LevelCard } from '@/components/rpg/level-card';
   import { SkillTree } from '@/components/rpg/skill-tree';

   export default function SkillsPage() {
     // Mock data until backend Phase 16
     const mockProgression = {
       level: 5,
       currentXP: 347,
       xpForNextLevel: 500,
       totalXP: 1847,
       unspentSkillPoints: 3,
     };

     const mockSkills = {
       FIRE: [
         {
           id: 'flame-breath-1',
           name: 'Flame Breath I',
           description: 'Basic fire attack, +10 damage',
           tree: 'FIRE' as const,
           currentRank: 2,
           maxRank: 2,
           levelRequired: 1,
           prerequisite: null,
           icon: '🔥',
         },
         {
           id: 'flame-breath-2',
           name: 'Flame Breath II',
           description: 'Improved fire attack, +25 damage',
           tree: 'FIRE' as const,
           currentRank: 1,
           maxRank: 3,
           levelRequired: 3,
           prerequisite: 'flame-breath-1',
           icon: '🔥',
         },
       ],
       SCALES: [
         {
           id: 'hardened-scales-1',
           name: 'Hardened Scales I',
           description: 'Increase defense by +15',
           tree: 'SCALES' as const,
           currentRank: 0,
           maxRank: 2,
           levelRequired: 1,
           prerequisite: null,
           icon: '🛡️',
         },
       ],
       AGILITY: [
         {
           id: 'quick-reflexes',
           name: 'Quick Reflexes',
           description: '+15% dodge chance',
           tree: 'AGILITY' as const,
           currentRank: 0,
           maxRank: 1,
           levelRequired: 1,
           prerequisite: null,
           icon: '⚡',
         },
       ],
     };

     const handleAllocate = (skillId: string) => {
       alert(`Skill allocation coming in Phase 16! Skill: ${skillId}`);
     };

     return (
       <div className="container mx-auto py-8">
         <h1 className="text-4xl font-bold mb-8">Skills & Progression</h1>

         <Alert className="mb-6">
           <AlertDescription>
             Full skill system will be available after backend Phase 16. This is a
             preview of the UI.
           </AlertDescription>
         </Alert>

         <div className="grid gap-6">
           <LevelCard {...mockProgression} />

           <Tabs defaultValue="FIRE">
             <TabsList className="grid w-full grid-cols-3">
               <TabsTrigger value="FIRE">🔥 Fire</TabsTrigger>
               <TabsTrigger value="SCALES">🛡️ Scales</TabsTrigger>
               <TabsTrigger value="AGILITY">⚡ Agility</TabsTrigger>
             </TabsList>

             {(['FIRE', 'SCALES', 'AGILITY'] as const).map((tree) => (
               <TabsContent key={tree} value={tree}>
                 <SkillTree
                   tree={tree}
                   skills={mockSkills[tree]}
                   unspentPoints={mockProgression.unspentSkillPoints}
                   onAllocate={handleAllocate}
                 />
               </TabsContent>
             ))}
           </Tabs>
         </div>
       </div>
     );
   }
   ```

5. **Dungeon Combat UI**
   ```typescript
   // src/components/rpg/dungeon-combat.tsx
   'use client';

   import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
   import { Button } from '@/components/ui/button';
   import { Progress } from '@/components/ui/progress';
   import { Badge } from '@/components/ui/badge';
   import { Sword, Shield, Heart } from 'lucide-react';

   interface DungeonCombatProps {
     floor: number;
     petHP: number;
     petMaxHP: number;
     enemies: Array<{
       id: string;
       type: string;
       currentHP: number;
       maxHP: number;
     }>;
     onAttack: (enemyId: string) => void;
     onRetreat: () => void;
   }

   export function DungeonCombat({
     floor,
     petHP,
     petMaxHP,
     enemies,
     onAttack,
     onRetreat,
   }: DungeonCombatProps) {
     return (
       <div className="space-y-6">
         <Card>
           <CardHeader>
             <div className="flex items-center justify-between">
               <CardTitle>Floor {floor}</CardTitle>
               <Badge variant="outline">Combat</Badge>
             </div>
           </CardHeader>
           <CardContent>
             <div className="space-y-2">
               <div className="flex items-center justify-between text-sm">
                 <span className="flex items-center gap-2">
                   <Heart className="h-4 w-4 text-red-500" />
                   Your HP
                 </span>
                 <span className="font-medium">
                   {petHP}/{petMaxHP}
                 </span>
               </div>
               <Progress value={(petHP / petMaxHP) * 100} />
             </div>
           </CardContent>
         </Card>

         <div className="space-y-4">
           <h3 className="text-lg font-semibold">Enemies</h3>
           {enemies.map((enemy) => (
             <Card key={enemy.id}>
               <CardContent className="pt-6">
                 <div className="flex items-center justify-between mb-4">
                   <div>
                     <p className="font-medium">{enemy.type}</p>
                     <p className="text-sm text-muted-foreground">
                       {enemy.currentHP}/{enemy.maxHP} HP
                     </p>
                   </div>
                   <Button onClick={() => onAttack(enemy.id)} size="sm">
                     <Sword className="h-4 w-4 mr-2" />
                     Attack
                   </Button>
                 </div>
                 <Progress value={(enemy.currentHP / enemy.maxHP) * 100} />
               </CardContent>
             </Card>
           ))}
         </div>

         <Button
           variant="outline"
           className="w-full"
           onClick={onRetreat}
         >
           <Shield className="h-4 w-4 mr-2" />
           Retreat
         </Button>
       </div>
     );
   }
   ```

6. **Dungeon Page**
   ```typescript
   // src/app/dungeon/page.tsx
   'use client';

   import { useState } from 'react';
   import { Button } from '@/components/ui/button';
   import { Alert, AlertDescription } from '@/components/ui/alert';
   import { DungeonCombat } from '@/components/rpg/dungeon-combat';

   export default function DungeonPage() {
     const [inCombat, setInCombat] = useState(false);

     // Mock combat data
     const mockCombat = {
       floor: 1,
       petHP: 180,
       petMaxHP: 200,
       enemies: [
         {
           id: 'goblin-1',
           type: 'Goblin',
           currentHP: 25,
           maxHP: 30,
         },
       ],
     };

     const handleStartDungeon = () => {
       alert('Dungeon system coming in Phase 17!');
       setInCombat(true);
     };

     const handleAttack = (enemyId: string) => {
       alert(`Attacking ${enemyId}`);
     };

     const handleRetreat = () => {
       setInCombat(false);
     };

     return (
       <div className="container mx-auto py-8 max-w-2xl">
         <h1 className="text-4xl font-bold mb-8">Dungeon</h1>

         <Alert className="mb-6">
           <AlertDescription>
             Full dungeon combat system will be available after backend Phase 17.
             This is a preview of the UI.
           </AlertDescription>
         </Alert>

         {!inCombat ? (
           <div className="text-center space-y-4">
             <p className="text-muted-foreground">
               Enter the dungeon to battle enemies and earn rewards!
             </p>
             <Button size="lg" onClick={handleStartDungeon}>
               Start Dungeon Run
             </Button>
           </div>
         ) : (
           <DungeonCombat
             {...mockCombat}
             onAttack={handleAttack}
             onRetreat={handleRetreat}
           />
         )}
       </div>
     );
   }
   ```

7. **Update Navigation**
   ```typescript
   // src/components/layout/nav-bar.tsx (final update)
   const navItems = [
     { href: '/', label: 'Dashboard' },
     { href: '/pets', label: 'My Pets' },
     { href: '/skills', label: 'Skills' },
     { href: '/dungeon', label: 'Dungeon' },
     { href: '/inventory', label: 'Inventory' },
     { href: '/games', label: 'Games' },
     { href: '/achievements', label: 'Achievements' },
     { href: '/leaderboard', label: 'Leaderboard' },
   ];
   ```

### Testing Checklist

- [ ] Level card displays XP progress
- [ ] Skill tree shows all skills correctly
- [ ] Can view different skill trees via tabs
- [ ] Skill allocation button appears
- [ ] Locked skills display correctly
- [ ] Dungeon combat UI displays HP bars
- [ ] Enemy cards show correctly
- [ ] Attack and retreat buttons work
- [ ] All navigation items present

### Technical Notes

- XP progress bar shows percentage to next level
- Skill trees organized by tabs
- Prerequisites lock skills visually
- Combat UI shows HP for pet and enemies
- Progress bars for all HP displays
- Mock data until backend Phases 15-19

---

## Deployment & Production

### Build Configuration

```json
// package.json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "type-check": "tsc --noEmit"
  }
}
```

### Environment Variables

```bash
# .env.production
NEXT_PUBLIC_API_BASE_URL=https://api.reactor-pets.com/api
NEXT_PUBLIC_POLLING_INTERVAL=10000
```

### Dockerfile

```dockerfile
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV production
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000
ENV PORT 3000
CMD ["node", "server.js"]
```

---

## Testing Strategy

### Unit Tests

```typescript
// tests/components/pet-card.test.tsx
import { render, screen } from '@testing-library/react';
import { PetCard } from '@/components/pets/pet-card';

describe('PetCard', () => {
  it('renders pet name', () => {
    const mockPet = {
      petId: '1',
      name: 'Fluffy',
      type: 'CAT',
      // ... other props
    };

    render(<PetCard pet={mockPet} />);
    expect(screen.getByText('Fluffy')).toBeInTheDocument();
  });
});
```

### E2E Tests

```typescript
// tests/e2e/create-pet.spec.ts
import { test, expect } from '@playwright/test';

test('create new pet', async ({ page }) => {
  await page.goto('/');
  await page.click('text=Create New Pet');
  await page.fill('input[name="name"]', 'TestPet');
  await page.selectOption('select[name="type"]', 'DOG');
  await page.click('button[type="submit"]');

  await expect(page.locator('text=TestPet')).toBeVisible();
});
```

---

## Implementation Checklist

### Phase 1: Foundation
- [ ] Initialize Next.js project
- [ ] Install dependencies
- [ ] Setup environment variables
- [ ] Create API client
- [ ] Setup React Query
- [ ] Initialize shadcn/ui
- [ ] Create root layout
- [ ] Test API connection

### Phase 2: Pet Management
- [ ] Create custom hooks for pets
- [ ] Build create pet dialog
- [ ] Implement pet card component
- [ ] Build pet list page
- [ ] Create pet detail page
- [ ] Add ASCII art display
- [ ] Test pet CRUD operations

### Phase 3: Dashboard & Statistics
- [ ] Create statistics hooks
- [ ] Build dashboard page
- [ ] Implement stats overview
- [ ] Create stage distribution chart
- [ ] Build leaderboard page
- [ ] Add navigation bar
- [ ] Test statistics display

### Phase 4: Real-time & Polish
- [ ] Add loading skeletons
- [ ] Create error boundary
- [ ] Build update indicator
- [ ] Implement stat animations
- [ ] Add toast notifications
- [ ] Enhance pet detail view
- [ ] Test real-time updates

### Phase 5: Inventory UI
- [ ] Define inventory types
- [ ] Create inventory grid
- [ ] Build inventory page
- [ ] Implement use item dialog
- [ ] Add to navigation
- [ ] Test inventory display

### Phase 6: Games & Achievements
- [ ] Define achievement types
- [ ] Create achievements page
- [ ] Build game card component
- [ ] Create games page
- [ ] Update navigation
- [ ] Test UI components

### Phase 7: Chat Interface
- [ ] Define chat types
- [ ] Create chat component
- [ ] Add chat to pet detail
- [ ] Build personality card
- [ ] Test chat interface

### Phase 8: RPG & Combat
- [ ] Define RPG types
- [ ] Create level card
- [ ] Build skill tree component
- [ ] Create skills page
- [ ] Implement dungeon combat UI
- [ ] Build dungeon page
- [ ] Update navigation
- [ ] Test RPG UI

---

## Future Enhancements

### Performance Optimizations
- Implement React Server Components where applicable
- Add image optimization for ASCII art rendering
- Use code splitting for large components
- Implement virtual scrolling for large lists

### Advanced Features
- WebSocket support for real-time updates (replace polling)
- Push notifications for critical events
- PWA support for mobile
- Offline mode with service workers
- Dark mode support
- Multi-language support (i18n)

### Analytics & Monitoring
- Integrate analytics (Vercel Analytics, Google Analytics)
- Error tracking (Sentry)
- Performance monitoring (Web Vitals)
- User behavior tracking

---

## Conclusion

This frontend implementation provides a complete, modern UI for the Reactor Pets application. The phased approach ensures:

1. **Immediate Value:** Core pet management works from day 1
2. **Future-Ready:** UI components prepared for all planned backend features
3. **Maintainable:** TypeScript, React Query, and shadcn/ui provide solid foundation
4. **Scalable:** Architecture supports adding new features easily
5. **Professional:** Production-ready with error handling, loading states, and animations

Each phase can be completed in a single Claude Code session, making the implementation manageable and iterative. The frontend is designed to work with the existing REST API and gracefully handle future backend additions.

Ready to bring your pets to life on the web? Start with Phase 1! 🚀
