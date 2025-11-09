# Reactor Pets - Virtual Pet (Tamagotchi)

A virtual pet application built with Axon Framework 4.x and Project Reactor, demonstrating Event Sourcing, CQRS, Saga pattern, and reactive programming.

## Technology Stack

<img width="1087" height="554" alt="Screenshot 2025-11-09 at 11 42 22 AM" src="https://github.com/user-attachments/assets/deabdf43-b69c-4e4a-a1a4-d2e968fb8005" />



https://github.com/user-attachments/assets/ea902554-e95a-490e-9b3e-b7b0fac7ea9e



- **Java**: 21+
- **Framework**: Spring Boot 3.2.0
- **Event Sourcing/CQRS**: Axon Framework 4.9.1
- **Reactive**: Project Reactor
- **Event Store**: Axon Server (Docker)
- **Build**: Maven
- **Code Quality**: Spotless, Checkstyle, SpotBugs, JaCoCo

## Prerequisites

- Java 21+
- Maven 3.x
- Docker & Docker Compose
- Node.js 24+ (for frontend)

## Quick Start

### 1. Start Axon Server

```bash
docker-compose up -d
```

Verify Axon Server is running at http://localhost:8024 (GUI) and localhost:8124 (gRPC)

### 2. Build & Run Backend

```bash
# Build
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run
```

The backend will start at http://localhost:8080

### 3. Start Frontend

```bash
# Navigate to frontend directory
cd reactor-pets-frontend

# Create environment file (first time only)
cp .env.local.example .env.local

# Install dependencies
yarn

# Start development server
npm run dev
```

The frontend will start at http://localhost:3000

## Testing

```bash
# Run all tests (102 tests)
mvn test

# Run with coverage report
mvn verify

# View coverage: target/site/jacoco/index.html
```

## Development Workflow

```bash
# Format code
mvn spotless:apply

# Run all checks
mvn clean verify
```

## Interfaces

### Web Frontend (React/Next.js)

URL: `http://localhost:3000`

Interactive web interface featuring:
- Visual pet display with 3D graphics
- Real-time pet stats monitoring
- Pet care actions (feed, play, clean)
- Global statistics and leaderboards
- Event history viewer

### REST API Endpoints

Base URL: `http://localhost:8080/api`

**Pet Operations:**
- `POST /api/pets` - Create a new pet
- `GET /api/pets` - List all pets
- `GET /api/pets/{id}` - Get pet status
- `POST /api/pets/{id}/feed` - Feed pet
- `POST /api/pets/{id}/play` - Play with pet
- `POST /api/pets/{id}/clean` - Clean pet
- `GET /api/pets/{id}/history?limit=10` - Get event history

**Statistics:**
- `GET /api/statistics` - Get global statistics
- `GET /api/leaderboard?type=AGE` - Get leaderboard (AGE, HAPPINESS, HEALTH)

**API Documentation:**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/v3/api-docs`

See `docs/06_REST_API.md` for detailed API documentation and examples.

## Architecture

Built with **Event Sourcing**, **CQRS**, and **Saga** patterns:

- **Aggregates:** `Pet` (write model)
- **Commands:** Create, Feed, Play, Clean, Evolve, TimeTick
- **Events:** PetCreated, PetFed, PetPlayedWith, PetCleaned, TimePassed, PetEvolved, PetDied
- **Projections:**
  - PetStatusProjection (JPA) - individual pet state
  - PetStatisticsProjection (JPA) - global statistics
  - PetHistoryProjection (EventStore) - event history
- **Saga:** PetEvolutionSaga (coordinates evolution based on age and care)
- **Reactive:** TimeTickScheduler (Flux.interval with concurrency control for batch processing)
- **Services:** PetManagerService (multi-pet management and statistics)
- **REST API:** PetController, StatisticsController with async operations
- **DTOs:** Request/Response objects with validation and error handling

**Core Features:**
- Event sourcing for complete pet history
- Reactive time system for automatic stat changes
- Saga pattern for multi-step evolution process
- Evolution paths determined by care quality
- Stage-based stat degradation rates

See `docs/01_DESIGN.md` for phase roadmap and future features.

## What You'll Learn

- **Event Sourcing** - State derived from immutable event streams
- **CQRS** - Separate command and query models
- **Saga Pattern** - Coordinating long-running processes
- **Axon Framework** - Aggregates, commands, events, sagas, projections
- **Project Reactor** - Reactive programming with Flux/Mono
- **Domain-Driven Design** - Bounded contexts, aggregates, domain events

## Next Steps

See `docs/01_DESIGN.md` for planned phases:
- **Phase 6: REST API** ✅ COMPLETE - Expose endpoints for web/mobile frontends
- **Phase 7 (Next): Items & Inventory System** - Food types, toys, medicine
- Phase 8: Mini-Games & Achievements
- Phase 9: Advanced Features (snapshots, upcasting, deadlines)

## License

Educational project - MIT License
