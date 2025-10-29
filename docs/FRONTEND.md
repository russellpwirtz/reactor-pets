# Reactor Pets - Frontend Application
## Next.js + TypeScript Implementation Plan

**Last Updated:** 2025-10-29
**Current Status:** Phases 1-7 Complete ✅ | Next: Phase 8 (Consumables UI - waiting on backend)

**Backend Alignment:**
- ✅ Backend Phase 7A Complete (XP System) - **Frontend Phase 5 Complete ✅**
- ✅ Backend Phase 7B Complete (Equipment System) - **Frontend Phase 6 Complete ✅**
- ✅ Backend Phase 7C Complete (Shop System) - **Frontend Phase 7 Complete ✅**
- 🚧 Backend Phase 7D In Progress (Consumables) - **Frontend Phase 8 pending backend REST endpoints**

**Prerequisites:** This document assumes the backend REST API is running with Phase 7A-7C features complete. The frontend consumes the REST endpoints for XP, equipment, shop, and consumables.

---

## Quick Reference

### Frontend-Backend Mapping

| Frontend Phase | Backend Phase | Status | Description |
|----------------|---------------|--------|-------------|
| Phase 1-4 ✅ | Phases 1-6 ✅ | Complete | Foundation, pets, stats, real-time updates |
| **Phase 5 ✅** | **7A ✅** | **Complete** | XP & Progression display |
| **Phase 6 ✅** | **7B ✅** | **Complete** | Equipment management UI |
| **Phase 7 ✅** | **7C ✅** | **Complete** | Shop interface |
| Phase 8 | 7D 🚧 | Waiting on backend | Consumables & inventory |
| Phase 9+ | 7E-9 📋 | Future | Achievements, chat, RPG |

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
│   │   ├── shop/              # Shop page
│   │   ├── leaderboard/       # Leaderboard view
│   │   └── api/               # API route handlers (if needed)
│   ├── components/            # React components
│   │   ├── ui/               # shadcn/ui components
│   │   ├── pets/             # Pet-specific components
│   │   ├── stats/            # Statistics components
│   │   ├── progression/      # XP and progression components
│   │   ├── shop/             # Shop components
│   │   ├── equipment/        # Equipment components
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

## Completed Phases ✅

### Phase 1: Project Foundation & Setup ✅
Next.js 15 project with TypeScript, Tailwind CSS, shadcn/ui, and React Query. Type-safe API client, environment configuration, and all core UI components installed.

**Location:** `/reactor-pets-frontend/`

### Phase 2: Pet Management UI ✅
Full CRUD operations for pets with real-time polling updates. Pet list/detail views, interaction buttons (feed/play/clean), event history, and ASCII art display.

**Components:** `src/components/pets/*`, `src/hooks/use-pets.ts`
**Pages:** `/pets`, `/pets/[id]`

### Phase 3: Dashboard & Statistics ✅
Global statistics dashboard with quick actions, stats overview cards, stage distribution chart, and leaderboard with Age/Happiness/Health sorting. Navigation bar with active page highlighting. "Alive Only" toggle filter for both leaderboard and My Pets pages.

**Components:** `src/components/stats/*`, `src/components/layout/nav-bar.tsx`
**Hooks:** `src/hooks/use-statistics.ts`
**Pages:** `/` (dashboard), `/leaderboard`
**Backend Enhancement:** Added `aliveOnly` parameter to leaderboard API

### Phase 4: Real-time Updates & Polish ✅
Real-time update indicators, smooth animations with Framer Motion, loading skeletons, error boundaries, and toast notifications for critical pet stats. Enhanced pet detail view with animated stat bars and automatic notifications.

**Components:** `src/components/ui/update-indicator.tsx`, `src/components/ui/skeleton.tsx`, `src/components/error-boundary.tsx`, `src/components/pets/animated-stat-bar.tsx`, `src/components/pets/pet-card-skeleton.tsx`, `src/components/pets/pet-detail-skeleton.tsx`
**Hooks:** `src/hooks/use-pet-notifications.ts`
**Enhancements:** Updated layout with UpdateIndicator, enhanced PetDetailView with animations, improved loading states across all pages

### Phase 5: XP & Progression UI ✅

**Status:** ✅ Complete
**Backend:** Phase 7A Complete (PlayerProgression aggregate, XP earning from interactions)

**Implemented Features:**
- Player progression types (`PlayerProgression`)
- `XPCard` component displaying current XP, lifetime XP, multiplier, and spending stats
- `useProgression` hook with automatic polling (5-second intervals)
- Integration with dashboard page
- Real-time XP updates from pet interactions

**Components:**
- `src/lib/types/progression.ts` - Type definitions
- `src/components/progression/xp-card.tsx` - XP display card
- `src/hooks/use-progression.ts` - Progression data hook

**Key Features:**
- Visual XP multiplier badge
- Highest multiplier tracking
- Total XP spent tracking
- Automatic refresh on interactions

**Backend Endpoints:**
- `GET /api/progression` - Get player progression stats

---

### Phase 6: Equipment System UI ✅

**Status:** ✅ Complete
**Backend:** Phase 7B Complete (Equipment aggregate, equip/unequip commands)

**Implemented Features:**
- Equipment types (9 items across 3 slots: Food Bowl, Toy, Accessory)
- Equipment slots display with stat modifiers
- Equipment inventory dialog
- Equip/unequip functionality per pet
- Real-time equipment updates via polling

**Components:**
- `src/lib/types/equipment.ts` - Type definitions
- `src/components/equipment/equipment-slots.tsx` - Equipment slot display
- `src/components/equipment/equipment-inventory-dialog.tsx` - Inventory selection dialog
- `src/hooks/use-equipment.ts` - Equipment hooks (usePetEquipment, useEquipmentInventory, useEquipItem, useUnequipItem)

**Key Features:**
- Visual slot icons (🍽️ Food Bowl, 🎾 Toy, 📿 Accessory)
- Modifier display (hunger decay, food efficiency, play efficiency, etc.)
- Inventory filtering by slot type
- Toast notifications for equip/unequip actions
- Equipment tab added to pet detail page (`/pets/[id]`)

**Backend Endpoints:**
- `GET /api/pets/{id}/equipment` - Get pet's equipped items
- `POST /api/pets/{id}/equipment/equip` - Equip item to slot
- `POST /api/pets/{id}/equipment/unequip` - Unequip item from slot
- `GET /api/inventory/equipment` - Get player's equipment inventory

---

### Phase 7: Shop System UI ✅

**Status:** ✅ Complete
**Backend:** Phase 7C Complete (Shop catalog, purchase saga, XP deduction)

**Implemented Features:**
- Shop types and catalog display
- Equipment and permanent upgrades tabs
- Purchase validation (XP balance checking)
- XP balance display on shop page
- Toast notifications for purchases
- Shop link added to navigation bar

**Components:**
- `src/lib/types/shop.ts` - Type definitions
- `src/components/shop/shop-grid.tsx` - Shop item grid
- `src/hooks/use-shop.ts` - Shop hooks (useShopItems, useShopUpgrades, usePurchaseEquipment, usePurchaseUpgrade, usePurchaseConsumable)
- `src/app/shop/page.tsx` - Shop page

**Key Features:**
- Item icons for visual identification
- XP cost display with affordability validation
- Disabled purchase buttons when insufficient XP
- Automatic XP balance refresh after purchases
- Separate tabs for equipment and permanent upgrades
- Item descriptions and slot information

**Backend Endpoints:**
- `GET /api/shop/items` - Get equipment items for sale
- `GET /api/shop/upgrades` - Get permanent upgrades
- `POST /api/shop/purchase/equipment/{equipmentType}` - Buy equipment
- `POST /api/shop/purchase/upgrade/{upgradeType}` - Buy upgrade
- `POST /api/shop/purchase/consumable/{consumableType}` - Buy consumable

**Shop Items:**
- **Equipment (9 items):** Basic Bowl, Large Bowl, Premium Bowl, Simple Ball, Interactive Toy, Luxury Toy Set, Basic Collar, Comfort Bed, Health Monitor
- **Permanent Upgrades (8 items):** Better Metabolism, Cheerful Disposition, Strong Genetics, Gourmet Kitchen, Rapid Hatcher, Multi-Pet Licenses (I, II, III)

---

## Phase 8: Consumables & Inventory UI

**Goal:** Display consumable inventory, use consumables on pets, and shop integration. Backend Phase 7D in progress.

**Backend Status:** 🚧 Phase 7D In Progress (Consumables implemented, REST/CLI pending)
**Duration Estimate:** Single Claude Code session (ready to implement once backend complete)

### Backend Endpoints (When Complete)
- `GET /api/inventory/consumables` - Get consumable inventory
- `POST /api/pets/{id}/consumable/{type}` - Use consumable on pet
- Consumables purchasable via `/api/shop/purchase/consumable/{type}`

### Planned Deliverables

1. **Consumable Types**
   ```typescript
   // src/lib/types/consumables.ts
   export type ConsumableType =
     | 'APPLE'        // 50 XP - Restores 15 hunger
     | 'PIZZA'        // 100 XP - Restores 30 hunger
     | 'GOURMET_MEAL' // 200 XP - Restores 50 hunger
     | 'BASIC_MEDICINE'    // 100 XP - Restores 20 health
     | 'ADVANCED_MEDICINE' // 200 XP - Restores 40 health, cures sickness
     | 'COOKIE'       // 75 XP - Restores 15 happiness
     | 'PREMIUM_TOY'; // 150 XP - Restores 30 happiness
   ```

2. **Consumables Grid Component** - Display consumable inventory with quantity tracking
3. **Use Consumable Dialog** - Select consumable and apply to pet
4. **Inventory Page** - View all consumables with use functionality
5. **Add consumables to shop** - Purchase consumables with XP

### Key Features (Planned)
- Consumable inventory with quantity display
- Category badges (Food, Medicine, Treat)
- Use button with quantity validation
- Equipment modifiers apply to consumables (FOOD_EFFICIENCY, PLAY_EFFICIENCY)
- Sickness indicator on pet detail
- Advanced Medicine cures sickness

---

## Phase 9: Mini-Games & Achievements UI

**Goal:** Create interactive mini-game interfaces and achievement display, preparing for Phase 8 backend.

**Backend Status:** ⏳ Pending (Backend Phase 8)
**Duration Estimate:** Single Claude Code session

### Planned Features
- Achievement display by category (Care, Progression, Equipment, Mastery)
- Achievement progress tracking
- Mini-game cards (Guess Game, Reflex Game, Emotional Intelligence)
- XP rewards and cooldown timers
- Integration with navigation

---

## Phase 10: Chat Interface (LLM-Ready)

**Goal:** Create chat interface for LLM integration, preparing for Phases 10-14 backend (Pet Personality & Chat).

**Backend Status:** ⏳ Pending (Backend Phases 10-14)
**Duration Estimate:** Single Claude Code session

### Planned Features
- Chat interface component
- Pet personality display (traits, mood, stability)
- Message history with auto-scrolling
- Personality badges
- Chat tab on pet detail page

---

## Phase 11: RPG & Combat UI (Future-Ready)

**Goal:** Create UI for XP, levels, skill trees, and dungeon combat, preparing for Phases 15-19 backend (RPG Progression).

**Backend Status:** ⏳ Pending (Backend Phases 15-19)
**Duration Estimate:** Single Claude Code session

### Planned Features
- Level and XP progress display
- Skill tree visualization (Fire, Scales, Agility)
- Skill allocation interface
- Dungeon combat UI
- Enemy cards with HP tracking
- Attack and retreat actions

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
# .env.local
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
NEXT_PUBLIC_POLLING_INTERVAL=5000

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

### Phase 1: Foundation ✅
- [x] Initialize Next.js project
- [x] Install dependencies
- [x] Setup environment variables
- [x] Create API client
- [x] Setup React Query
- [x] Initialize shadcn/ui
- [x] Create root layout
- [x] Test API connection

### Phase 2: Pet Management ✅
- [x] Create custom hooks for pets
- [x] Build create pet dialog
- [x] Implement pet card component
- [x] Build pet list page
- [x] Create pet detail page
- [x] Add ASCII art display
- [x] Test pet CRUD operations

### Phase 3: Dashboard & Statistics ✅
- [x] Create statistics hooks
- [x] Build dashboard page
- [x] Implement stats overview
- [x] Create stage distribution chart
- [x] Build leaderboard page
- [x] Add navigation bar
- [x] Add "Alive Only" toggle to leaderboard and My Pets
- [x] Test statistics display

### Phase 4: Real-time & Polish ✅
- [x] Add loading skeletons
- [x] Create error boundary
- [x] Build update indicator
- [x] Implement stat animations
- [x] Add toast notifications
- [x] Enhance pet detail view
- [x] Test real-time updates

### Phase 5: XP & Progression UI ✅
- [x] Define progression types
- [x] Create XP card component
- [x] Create custom hook for progression
- [x] Add XP card to dashboard
- [x] Test XP updates on pet interactions
- [x] Verify multiplier calculations

### Phase 6: Equipment System UI ✅
- [x] Define equipment types
- [x] Create equipment slots component
- [x] Build equipment inventory dialog
- [x] Add equipment tab to pet detail page
- [x] Implement equip/unequip functionality
- [x] Test equipment modifiers display

### Phase 7: Shop System UI ✅
- [x] Define shop types
- [x] Create shop grid component
- [x] Build shop page with tabs
- [x] Integrate XP balance display
- [x] Implement purchase flow
- [x] Add shop to navigation
- [x] Test purchase validation

### Phase 8: Consumables & Inventory UI (Pending Backend)
- [ ] Define consumable types
- [ ] Create consumables grid component
- [ ] Build inventory page
- [ ] Implement use consumable functionality
- [ ] Add consumables to shop
- [ ] Test consumable effects
- [ ] Display sickness indicator

### Future Phases (Backend Pending)

### Phase 9: Mini-Games & Achievements UI
- [ ] Achievement types and display
- [ ] Game card components
- [ ] Achievements page
- [ ] Games page

### Phase 10: Chat Interface (LLM-Ready)
- [ ] Chat types
- [ ] Chat component
- [ ] Personality display

### Phase 11: RPG & Combat UI
- [ ] RPG types
- [ ] Level card
- [ ] Skill tree component
- [ ] Dungeon combat UI

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

## Development Workflow

### Running the Application

```bash
# Development
npm run dev

# Type checking
npm run type-check

# Production build
npm run build
npm start
```

### API Configuration

The frontend connects to the backend API via environment variables. Ensure the backend is running before starting the frontend.

**Default Development Settings:**
- Backend API: `http://localhost:8080/api`
- Polling Interval: 5000ms (5 seconds)

---

## Success Metrics

### Phases 1-7 Success ✅
- [x] Player can earn XP from interactions
- [x] Equipment modifies pet stats with trade-offs
- [x] Can purchase equipment and upgrades with XP
- [x] Player progression tracked and persisted
- [x] Shop interface functional with purchase validation
- [x] Equipment can be equipped/unequipped per pet
- [x] Real-time updates for stats, XP, and equipment

### Phase 8 Success (Pending)
- [ ] Consumables provide immediate benefits
- [ ] Inventory displays quantities correctly
- [ ] Consumables can be used on pets
- [ ] Sickness indicator displays
- [ ] Advanced Medicine cures sickness

### Future Success Metrics
- [ ] Achievements unlock with XP bonuses
- [ ] Mini-games provide alternative XP source
- [ ] Chat interface functional with pet personality
- [ ] Skill trees allow character progression
- [ ] Dungeon combat system operational
