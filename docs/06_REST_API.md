# Phase 6: REST API - Complete ✅

## Summary

Phase 6 has been successfully implemented, adding a comprehensive REST API to the Reactor Pets application. The API exposes all pet management operations and statistics through RESTful endpoints.

## What's New

### 1. REST Controllers

#### PetController (`/api/pets`)
- **POST /api/pets** - Create a new pet
- **GET /api/pets** - List all pets
- **GET /api/pets/{id}** - Get pet status
- **POST /api/pets/{id}/feed** - Feed pet
- **POST /api/pets/{id}/play** - Play with pet
- **POST /api/pets/{id}/clean** - Clean pet
- **GET /api/pets/{id}/history?limit=10** - Get event history

#### StatisticsController (`/api`)
- **GET /api/statistics** - Get global statistics
- **GET /api/leaderboard?type=AGE** - Get leaderboard (types: AGE, HAPPINESS, HEALTH)

### 2. DTOs (Data Transfer Objects)

#### Request DTOs
- `CreatePetRequest` - For creating new pets with validation

#### Response DTOs
- `PetStatusResponse` - Complete pet status with ASCII art
- `StatisticsResponse` - Global statistics dashboard
- `LeaderboardResponse` - Top 10 pets by metric
- `PetHistoryResponse` - Event history with timestamps
- `ErrorResponse` - Standardized error responses

### 3. Error Handling

Global exception handler (`GlobalExceptionHandler`) provides:
- HTTP 400 for invalid requests and validation errors
- HTTP 404 for pets not found
- HTTP 500 for server errors
- Standardized JSON error responses with timestamps and paths

### 4. CORS Configuration

Configured to allow requests from:
- http://localhost:3000 (Next.js dev server)
- http://localhost:5173 (Vite dev server)
- http://localhost:8080 (Alternative dev server)

### 5. API Documentation (Swagger/OpenAPI)

- Interactive API documentation at `/swagger-ui.html`
- OpenAPI specification available
- Detailed endpoint descriptions and example requests/responses
- Try-it-out functionality for testing endpoints

## Technical Implementation

### Architecture

```
src/main/java/com/reactor/pets/api/
├── controller/
│   ├── PetController.java          # Pet operations endpoints
│   └── StatisticsController.java   # Statistics endpoints
├── dto/
│   ├── CreatePetRequest.java       # Request DTO with validation
│   ├── PetStatusResponse.java      # Pet status response
│   ├── StatisticsResponse.java     # Statistics response
│   ├── LeaderboardResponse.java    # Leaderboard response
│   ├── PetHistoryResponse.java     # Event history response
│   └── ErrorResponse.java          # Error response
├── exception/
│   └── GlobalExceptionHandler.java # Global error handling
└── config/
    ├── CorsConfig.java             # CORS configuration
    └── OpenApiConfig.java          # Swagger/OpenAPI config
```

### Key Features

1. **Async Operations**
   - All controller methods return `CompletableFuture` for non-blocking operations
   - Leverages Axon Framework's async command and query gateways

2. **Validation**
   - Request DTOs use Jakarta validation annotations
   - `@NotNull`, `@NotBlank` for required fields
   - Validation errors return HTTP 400 with detailed messages

3. **Error Handling**
   - Unwraps Axon exceptions (`CommandExecutionException`, `AggregateNotFoundException`)
   - Handles `CompletionException` from async operations
   - Returns consistent error format with timestamps

4. **Documentation**
   - Swagger annotations on all endpoints
   - Parameter descriptions and examples
   - Response code documentation

## API Examples

### Create a Pet

```bash
curl -X POST http://localhost:8080/api/pets \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Fluffy",
    "type": "CAT"
  }'
```

Response (201 Created):
```json
{
  "petId": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Fluffy",
  "type": "CAT",
  "stage": "EGG",
  "evolutionPath": "UNDETERMINED",
  "isAlive": true,
  "age": 0,
  "totalTicks": 0,
  "hunger": 30,
  "happiness": 70,
  "health": 100,
  "lastUpdated": "2025-10-28T16:00:00Z",
  "asciiArt": "..."
}
```

### Feed a Pet

```bash
curl -X POST http://localhost:8080/api/pets/{petId}/feed
```

### Get Pet Status

```bash
curl http://localhost:8080/api/pets/{petId}
```

### List All Pets

```bash
curl http://localhost:8080/api/pets
```

### Get Statistics

```bash
curl http://localhost:8080/api/statistics
```

Response (200 OK):
```json
{
  "totalPetsCreated": 42,
  "totalPetsDied": 5,
  "currentlyAlive": 37,
  "averageLifespan": 125.5,
  "longestLivedPetName": "Max",
  "longestLivedPetId": "abc-123",
  "longestLivedPetAge": 250,
  "stageDistribution": {
    "EGG": 10,
    "BABY": 15,
    "TEEN": 8,
    "ADULT": 4
  },
  "lastUpdated": "2025-10-28T16:00:00Z"
}
```

### Get Leaderboard

```bash
curl http://localhost:8080/api/leaderboard?type=AGE
```

## Testing

All existing tests continue to pass (117 tests total):
- Aggregate tests
- Saga tests
- Projection tests
- Service tests
- Integration tests

Build verification:
- ✅ Checkstyle: 0 violations
- ✅ SpotBugs: 0 bugs found
- ✅ Spotless: All files formatted
- ✅ JaCoCo: Coverage requirements met
- ✅ All 117 tests passing

## Dependencies Added

```xml
<!-- Spring Web for REST API -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Springdoc OpenAPI (Swagger) -->
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.3.0</version>
</dependency>
```

## Running the Application

1. Start Axon Server:
```bash
docker-compose up -d
```

2. Run the application:
```bash
mvn spring-boot:run
```

3. Access the API:
   - Base URL: http://localhost:8080/api
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - OpenAPI Spec: http://localhost:8080/v3/api-docs

## Next Steps

With the REST API complete, you can now:

1. **Frontend Development**
   - Build a Next.js/React frontend
   - Use the Swagger UI to understand available endpoints
   - Implement real-time updates with polling or WebSockets (future phase)

2. **Phase 7: Items System & Inventory**
   - Add food types, toys, and medicine
   - Implement inventory management
   - Cross-aggregate coordination with sagas

3. **Phase 8: Mini-Games & Achievements**
   - Interactive mini-games
   - Achievement tracking
   - Enhanced gameplay mechanics

## Notes

- CLI functionality remains fully operational alongside the REST API
- Both CLI and REST API use the same underlying services and command/query gateways
- The API is production-ready with proper error handling and documentation
- CORS is configured for local development (adjust for production)

## CLI vs REST API

Both interfaces are available:

### CLI
```bash
mvn spring-boot:run
> create Fluffy CAT
> feed <petId>
> status <petId>
```

### REST API
```bash
curl -X POST http://localhost:8080/api/pets -d '{"name":"Fluffy","type":"CAT"}'
curl -X POST http://localhost:8080/api/pets/{petId}/feed
curl http://localhost:8080/api/pets/{petId}
```

---

**Phase 6 Status:** ✅ **COMPLETE**

All deliverables implemented:
- ✅ REST Controllers for pet operations
- ✅ Statistics and leaderboard endpoints
- ✅ DTOs with validation
- ✅ Global exception handling
- ✅ CORS configuration
- ✅ Swagger/OpenAPI documentation
- ✅ All tests passing
- ✅ Build verification successful
