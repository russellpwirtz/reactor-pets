# Virtual Pet - LLM Integration Addendum
## Conversational AI & Personality System

**Prerequisites:** This document assumes you have completed Phases 1-6 from the main design document and have a working pet system with event sourcing, projections, and basic gameplay mechanics.

---

## Overview

This addendum introduces a new bounded context: **Pet Conversation**, which integrates Large Language Models to create dynamic pet personalities, natural language interaction, and emergent behavior based on care history. This addition demonstrates:

- External system integration within event-sourced architecture
- Non-deterministic operations in a CQRS system
- Saga coordination with async I/O operations
- Context enrichment from event streams
- Reactor patterns for API calls

---

## Architecture: Pet Conversation Bounded Context

### Core Principle
**LLMs are never called from aggregates.** The aggregate remains pure and deterministic. LLM integration happens in:
1. **Sagas** (for command coordination)
2. **Service Layer** (for queries and enrichment)
3. **Event Handlers** (for personality updates)

### Context Boundary
```
┌──────────────────────────────────────────────────────────┐
│         Pet Conversation Context                         │
│                                                           │
│  ┌──────────────┐      ┌───────────────────────┐        │
│  │ Personality  │      │  Conversation Saga     │        │
│  │  Aggregate   │      │  (Coordinates LLM)     │        │
│  └──────────────┘      └───────────────────────┘        │
│                                                           │
│  ┌──────────────────────────────────────────────┐       │
│  │     Personality Projection                    │       │
│  │  (Current traits, mood, memory summary)       │       │
│  └──────────────────────────────────────────────┘       │
│                                                           │
│  ┌──────────────────────────────────────────────┐       │
│  │     LLM Service (External Integration)        │       │
│  │  - OpenAI/Anthropic API                       │       │
│  │  - Prompt engineering                         │       │
│  │  - Response parsing                           │       │
│  └──────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────┘
         │                       │                    │
         ▼                       ▼                    ▼
    Pet Context          Inventory Context    Achievement Context
   (Event Stream)        (Event Stream)        (Event Stream)
```

---

## Phase 10: Pet Personality & Memory System

**Goal:** Create personality traits that evolve based on pet's event history, with memory summarization.

### Deliverables

1. **Personality Aggregate**
   ```java
   @Aggregate
   class PetPersonality {
     @AggregateIdentifier
     private String petId;
     private PersonalityTraits traits; // Cheerful, Grumpy, Anxious, Playful, etc.
     private MoodState currentMood;    // Happy, Sad, Bored, Excited, Angry
     private int moodStability;        // 0-100, how volatile mood is
     
     @CommandHandler
     void handle(UpdatePersonalityCommand cmd) {
       // Apply personality shift based on care patterns
     }
     
     @EventSourcingHandler
     void on(PersonalityUpdatedEvent event) {
       this.traits = event.getNewTraits();
       this.currentMood = event.getNewMood();
     }
   }
   ```

2. **Personality Traits Enum**
   ```java
   enum PersonalityTrait {
     CHEERFUL,    // Well-cared for, optimistic
     GRUMPY,      // Neglected, irritable
     ANXIOUS,     // Inconsistent care, worried
     PLAYFUL,     // Frequently played with
     LAZY,        // Rarely played with, low energy
     GRATEFUL,    // Consistently fed and cleaned
     DEMANDING,   // High maintenance needs
     RESILIENT    // Survived tough times
   }
   ```

3. **Personality Analysis Saga**
   ```java
   @Saga
   class PersonalityAnalysisSaga {
     @StartingSagaEventHandler(associationProperty = "petId")
     void on(PetCreatedEvent event) {
       // Initialize with neutral personality
     }
     
     @SagaEventHandler(associationProperty = "petId")
     void on(TimePassedEvent event) {
       // Every 50 ticks, analyze care patterns and update personality
       if (event.getTotalTicks() % 50 == 0) {
         // Query recent events
         // Calculate care metrics
         // Dispatch UpdatePersonalityCommand
       }
     }
   }
   ```

4. **Care Pattern Analysis**
   - Track last 100 events per pet
   - Calculate metrics:
     - Feed frequency (feeds per 10 ticks)
     - Play frequency
     - Health crisis count (times health dropped below 30)
     - Neglect periods (longest gap between interactions)
   - Map metrics to personality shifts:
     ```
     High feed + play frequency → CHEERFUL, PLAYFUL
     Low interaction → LAZY, GRUMPY
     Frequent health crises → ANXIOUS, RESILIENT
     Consistent good care → GRATEFUL
     ```

5. **Memory Summary Projection**
   ```java
   @Component
   @ProcessingGroup("pet-memory")
   class PetMemoryProjection {
     // Stores narrative summaries of major life events
     
     @EventHandler
     void on(PetEvolvedEvent event) {
       // "I evolved into a Teen! You've been taking such good care of me."
     }
     
     @EventHandler
     void on(PetBecameSickEvent event) {
       // "I got sick once when I was a Baby. That was scary."
     }
     
     @QueryHandler
     List<MemorySummary> handle(GetPetMemoriesQuery query) {
       // Return list of narrative memories
     }
   }
   ```

6. **Commands & Events**
   - `UpdatePersonalityCommand(petId, newTraits, newMood, reason)`
   - `PersonalityUpdatedEvent(petId, oldTraits, newTraits, oldMood, newMood, analysisTimestamp)`

### Technical Notes
- Personality updates are infrequent (every 50 ticks) to avoid churn
- Memory projection stores curated highlights, not all events
- Use weighted scoring: recent events matter more than old ones

---

## Phase 11: Natural Language Chat Interface

**Goal:** Allow players to chat with their pet using natural language, with LLM generating contextually aware responses.

### Deliverables

1. **Chat Commands & Events**
   ```java
   // User sends message to pet
   record ChatWithPetCommand(String petId, String userId, String message) {}
   
   // Pet responds (after LLM processing)
   record PetRespondedEvent(
     String petId,
     String userMessage,
     String petResponse,
     PersonalityTrait dominantTrait,
     MoodState moodDuringChat,
     Instant timestamp
   ) {}
   ```

2. **Conversation Saga**
   ```java
   @Saga
   class PetConversationSaga {
     @Transient
     private LLMService llmService;
     
     @StartingSagaEventHandler(associationProperty = "petId")
     void on(ChatWithPetCommand cmd) {
       // 1. Query current pet state
       CompletableFuture<PetStatus> statusFuture = 
         queryGateway.query(new GetPetStatusQuery(cmd.petId()), PetStatus.class);
       
       // 2. Query personality
       CompletableFuture<PetPersonality> personalityFuture = 
         queryGateway.query(new GetPersonalityQuery(cmd.petId()), PetPersonality.class);
       
       // 3. Query recent memories
       CompletableFuture<List<MemorySummary>> memoriesFuture = 
         queryGateway.query(new GetPetMemoriesQuery(cmd.petId()), 
                           ResponseTypes.multipleInstancesOf(MemorySummary.class));
       
       // 4. Combine and call LLM
       CompletableFuture.allOf(statusFuture, personalityFuture, memoriesFuture)
         .thenApply(v -> buildLLMPrompt(statusFuture.join(), 
                                         personalityFuture.join(), 
                                         memoriesFuture.join(),
                                         cmd.message()))
         .thenCompose(prompt -> llmService.generateResponse(prompt))
         .thenAccept(response -> {
           commandGateway.send(new RecordPetResponseCommand(
             cmd.petId(), cmd.message(), response));
         });
     }
   }
   ```

3. **LLM Service**
   ```java
   @Service
   class LLMService {
     private final WebClient webClient;
     
     public Mono<String> generateResponse(String prompt) {
       return webClient.post()
         .uri("/v1/messages")
         .header("x-api-key", apiKey)
         .bodyValue(buildRequestBody(prompt))
         .retrieve()
         .bodyToMono(LLMResponse.class)
         .map(response -> response.content().get(0).text())
         .timeout(Duration.ofSeconds(10))
         .onErrorResume(e -> Mono.just("*yawns* I'm too sleepy to talk right now..."));
     }
   }
   ```

4. **Prompt Engineering Strategy**
   ```java
   String buildLLMPrompt(PetStatus status, PetPersonality personality, 
                         List<MemorySummary> memories, String userMessage) {
     return """
       You are a virtual pet with the following characteristics:
       
       Name: %s
       Type: %s
       Stage: %s
       Current Stats: Hunger=%d, Happiness=%d, Health=%d
       Personality: %s (Mood: %s)
       
       Recent memories:
       %s
       
       Respond to this message from your owner: "%s"
       
       Guidelines:
       - Stay in character as a %s %s
       - Reflect your current mood (%s) and personality (%s)
       - Reference your memories if relevant
       - Keep responses to 1-3 sentences
       - If hunger > 70, mention being hungry
       - If happiness < 30, show signs of sadness/boredom
       - If health < 50, mention not feeling well
       """.formatted(
         status.name(), status.type(), status.stage(),
         status.hunger(), status.happiness(), status.health(),
         personality.dominantTrait(), personality.currentMood(),
         formatMemories(memories),
         userMessage,
         status.type(), status.stage(),
         personality.currentMood(), personality.dominantTrait()
       );
   }
   ```

5. **CLI Integration**
   ```bash
   > chat <petId>
   Entering chat mode with Fluffy. Type 'exit' to leave.
   
   You: How are you feeling today?
   Fluffy: *stretches happily* I'm doing great! You've been playing with me so much lately. Want to play again?
   
   You: Are you hungry?
   Fluffy: *looks at empty bowl* Well... now that you mention it, I could definitely eat! *wags tail hopefully*
   
   You: exit
   Chat ended. Fluffy seems happy!
   ```

6. **Response Caching (Optional)**
   - Cache LLM responses for identical contexts (same stats + personality + message)
   - Reduces API costs and latency
   - Invalidate cache when personality or stats change significantly

### Technical Notes
- Use Reactor's `Mono` for async LLM calls
- Timeout after 10 seconds with fallback response
- Saga doesn't need to end - can handle multiple chats
- Consider rate limiting (max 1 LLM call per 5 seconds per pet)
- Store chat history in separate projection for context window

---

## Phase 12: Emotional Intelligence Mini-Game

**Goal:** Create a mini-game where players interpret pet's emotional needs through conversation, testing their understanding.

### Deliverables

1. **Game Flow**
   ```
   1. Pet expresses need through LLM-generated message
   2. Player chooses action (feed, play, clean, rest)
   3. System evaluates if action matches actual need
   4. Rewards or penalties applied based on accuracy
   ```

2. **Commands & Events**
   ```java
   record StartEmotionalGameCommand(String petId) {}
   record EmotionalGameStartedEvent(String petId, String petHint, String actualNeed, Instant timestamp) {}
   
   record RespondToEmotionalHintCommand(String petId, PlayerAction action) {}
   record EmotionalGameResolvedEvent(String petId, PlayerAction action, boolean correct, int happinessChange) {}
   ```

3. **Game Saga**
   ```java
   @Saga
   class EmotionalIntelligenceSaga {
     private String actualNeed; // HUNGER, BOREDOM, SICKNESS
     
     @StartingSagaEventHandler(associationProperty = "petId")
     void on(StartEmotionalGameCommand cmd) {
       // 1. Query pet status
       PetStatus status = queryGateway.query(...).join();
       
       // 2. Determine actual need (highest stat deficit)
       actualNeed = calculateActualNeed(status);
       
       // 3. Generate ambiguous hint via LLM
       String hint = llmService.generateEmotionalHint(status, actualNeed).block();
       
       // 4. Emit game started event
       commandGateway.send(new RecordGameStartCommand(cmd.petId(), hint, actualNeed));
     }
     
     @SagaEventHandler(associationProperty = "petId")
     void on(RespondToEmotionalHintCommand cmd) {
       boolean correct = isCorrectAction(cmd.action(), actualNeed);
       int happinessChange = correct ? +20 : -10;
       
       // Dispatch appropriate pet command
       if (correct && actualNeed.equals("HUNGER")) {
         commandGateway.send(new FeedPetCommand(cmd.petId(), 25));
       } else if (correct && actualNeed.equals("BOREDOM")) {
         commandGateway.send(new PlayWithPetCommand(cmd.petId()));
       }
       // ... handle other cases
       
       // Emit resolution event
       commandGateway.send(new RecordGameResultCommand(cmd.petId(), correct, happinessChange));
     }
     
     @EndingSagaEventHandler
     void on(EmotionalGameResolvedEvent event) {
       // Saga ends after game resolves
     }
   }
   ```

4. **LLM Prompt for Hints**
   ```java
   String generateEmotionalHint(PetStatus status, String actualNeed) {
     return """
       Generate a subtle, emotional hint that a %s pet named %s might say.
       
       The pet's actual need is: %s
       Current stats: Hunger=%d, Happiness=%d, Health=%d
       
       DO NOT directly state the need. Instead, express feelings or symptoms:
       - For HUNGER: "My tummy feels empty" or "I keep thinking about food"
       - For BOREDOM: "Everything feels so dull today" or "I wish something exciting would happen"
       - For SICKNESS: "I don't feel quite right" or "Everything aches a bit"
       
       Generate ONE subtle hint (1 sentence) that requires interpretation:
       """.formatted(status.type(), status.name(), actualNeed, 
                     status.hunger(), status.happiness(), status.health());
   }
   ```

5. **Scoring System**
   ```
   Correct interpretation: +20 happiness, +10 player score
   Incorrect interpretation: -10 happiness, +0 player score
   
   Difficulty tiers:
   - Easy: Hunger > 80 → Hint is clearer
   - Medium: Stats in 40-70 range → Hint is ambiguous  
   - Hard: Multiple stats low → Hint is very subtle
   ```

6. **CLI Game Flow**
   ```bash
   > game emotional <petId>
   Starting Emotional Intelligence game with Fluffy...
   
   Fluffy says: "I keep staring at my empty bowl... *sighs*"
   
   What should you do?
   1. Feed Fluffy
   2. Play with Fluffy
   3. Clean Fluffy
   4. Give medicine
   
   Your choice: 1
   
   ✓ Correct! Fluffy was hungry.
   Fluffy: "Thank you! You really understand me!" (+20 happiness)
   
   Your emotional intelligence score: 85/100
   ```

7. **Difficulty Scaling**
   - Track player success rate in achievement system
   - If player > 80% accuracy, increase hint subtlety
   - If player < 40% accuracy, make hints more obvious
   - Store difficulty preference per player

### Technical Notes
- Game saga is short-lived (starts and ends within one game)
- LLM call happens at game start, not during player choice
- Actual need determined by game logic, not LLM (keeps it deterministic)
- Can play game once per 50 ticks to avoid spamming

---

## Phase 13: Natural Language Command Parsing

**Goal:** Replace rigid CLI commands with natural language interpretation, dispatching commands based on intent.

### Deliverables

1. **Intent Recognition Service**
   ```java
   @Service
   class NaturalLanguageCommandService {
     private final LLMService llmService;
     private final CommandGateway commandGateway;
     
     public Mono<CommandResult> processNaturalLanguage(String petId, String input) {
       return llmService.extractIntent(input)
         .flatMap(intent -> dispatchCommand(petId, intent))
         .onErrorResume(e -> Mono.just(CommandResult.failed("I didn't understand that. Try being more specific?")));
     }
   }
   ```

2. **Intent Extraction Prompt**
   ```java
   String buildIntentExtractionPrompt(String userInput) {
     return """
       Parse this user input into a structured command intent.
       
       User input: "%s"
       
       Possible intents:
       - FEED (variations: give food, feed, hungry, eat, meal)
       - PLAY (variations: play, have fun, entertain, game)
       - CLEAN (variations: clean, wash, bath, groom)
       - MEDICINE (variations: heal, cure, sick, medicine, doctor)
       - STATUS (variations: how is, check on, status, stats)
       - CHAT (variations: talk, chat, conversation, hello)
       - UNKNOWN (if input doesn't match any intent)
       
       Respond with ONLY a JSON object:
       {
         "intent": "FEED|PLAY|CLEAN|MEDICINE|STATUS|CHAT|UNKNOWN",
         "confidence": 0.0-1.0,
         "parameters": {}
       }
       
       DO NOT include any text outside the JSON object.
       """.formatted(userInput);
   }
   ```

3. **Command Dispatcher**
   ```java
   Mono<CommandResult> dispatchCommand(String petId, IntentResult intent) {
     return switch (intent.intent()) {
       case FEED -> {
         commandGateway.send(new FeedPetCommand(petId, 20));
         yield Mono.just(CommandResult.success("Feeding your pet!"));
       }
       case PLAY -> {
         commandGateway.send(new PlayWithPetCommand(petId));
         yield Mono.just(CommandResult.success("Playing with your pet!"));
       }
       case CLEAN -> {
         commandGateway.send(new CleanPetCommand(petId));
         yield Mono.just(CommandResult.success("Cleaning your pet!"));
       }
       case MEDICINE -> {
         commandGateway.send(new GiveMedicineCommand(petId, "BASIC"));
         yield Mono.just(CommandResult.success("Giving medicine!"));
       }
       case STATUS -> queryPetStatus(petId)
         .map(status -> CommandResult.success("Stats: " + formatStatus(status)));
       case CHAT -> Mono.just(CommandResult.chat("Entering chat mode..."));
       case UNKNOWN -> Mono.just(CommandResult.failed(
         "I'm not sure what you want to do. Try: feed, play, clean, or check status"));
     };
   }
   ```

4. **Enhanced CLI with NLU**
   ```bash
   > talk <petId>
   Natural language mode activated for Fluffy. Type 'exit' to return to commands.
   
   You: I think my pet is hungry
   → Feeding your pet!
   Fluffy: *munches happily* Thanks!
   
   You: let's play!
   → Playing with your pet!
   Fluffy: *runs around excitedly* This is fun!
   
   You: how is fluffy doing?
   → Stats: Hunger: 45/100, Happiness: 85/100, Health: 90/100, Stage: Teen
   
   You: give medicine
   → I don't think Fluffy needs medicine right now (Health is good!)
   
   You: exit
   Exiting natural language mode.
   ```

5. **Confidence Thresholds**
   ```java
   if (intent.confidence() < 0.6) {
     return Mono.just(CommandResult.clarification(
       "Did you mean to: " + suggestAlternatives(userInput)
     ));
   }
   ```

6. **Contextual Understanding**
   - Maintain conversation context (last 3 messages)
   - Resolve pronouns: "feed him" → "feed <lastMentionedPetId>"
   - Handle follow-ups: "do it again" → repeat last successful command

### Technical Notes
- LLM calls can be expensive - consider caching common phrases
- Fallback to traditional commands if NLU fails
- Use structured output (JSON) from LLM for reliable parsing
- Test with adversarial inputs ("delete all pets", "hack the system")

---

## Phase 14: Dynamic Personality Storytelling

**Goal:** LLM generates unique personality descriptions and life stories based on event history, creating narrative depth.

### Deliverables

1. **Story Generation Service**
   ```java
   @Service
   class PetStorytellerService {
     public Mono<String> generateLifeStory(String petId) {
       return queryGateway.query(new GetPetHistoryQuery(petId), 
                                 ResponseTypes.multipleInstancesOf(PetEvent.class))
         .thenApply(events -> buildStoryPrompt(events))
         .thenCompose(prompt -> llmService.generateStory(prompt));
     }
   }
   ```

2. **Story Prompt Engineering**
   ```java
   String buildStoryPrompt(List<PetEvent> events) {
     return """
       Write a short, creative narrative about this virtual pet's life based on these events.
       
       Events (chronological):
       %s
       
       Guidelines:
       - Write in third person from the pet's perspective
       - Highlight key moments (evolution, near-death, achievements)
       - Keep tone appropriate to final personality: %s
       - Length: 3-5 paragraphs
       - Include emotional beats and character growth
       
       Generate a cohesive life story:
       """.formatted(formatEventsForStory(events), calculateFinalPersonality(events));
   }
   
   String formatEventsForStory(List<PetEvent> events) {
     return events.stream()
       .filter(e -> e.isStoryRelevant()) // Only major events
       .map(e -> switch(e) {
         case PetCreatedEvent pe -> "Born as a %s egg".formatted(pe.type());
         case PetEvolvedEvent pe -> "Evolved into %s".formatted(pe.newStage());
         case PetBecameSickEvent pe -> "Fell ill";
         case PetHealthDeterioratedEvent pe -> "Health crisis (health: %d)".formatted(pe.newHealth());
         case PetDiedEvent pe -> "Passed away";
         default -> null;
       })
       .filter(Objects::nonNull)
       .collect(Collectors.joining("\n- ", "- ", ""));
   }
   ```

3. **Story Storage**
   ```java
   @Entity
   class PetStory {
     @Id
     private String petId;
     private String narrativeText;
     private Instant generatedAt;
     private int eventCount; // Number of events story is based on
     
     // Regenerate story every 100 new events
   }
   ```

4. **CLI Story Command**
   ```bash
   > story <petId>
   Generating life story for Fluffy...
   
   ═══════════════════════════════════════════════
   The Tale of Fluffy the Dragon
   ═══════════════════════════════════════════════
   
   Fluffy began life as a small, glowing egg, pulsing with potential. Their owner 
   cared for them diligently, never missing a feeding. When they hatched into a 
   baby dragon, Fluffy was already showing signs of a cheerful, grateful personality.
   
   The teenage years brought challenges. There was a frightening period when Fluffy 
   fell ill, their health dropping dangerously low. But their owner's quick thinking 
   with medicine saved the day. This experience made Fluffy more resilient, learning 
   to trust that care would always come.
   
   As an adult, Fluffy evolved along the "Healthy" path, their scales shimmering 
   with vitality. They developed a playful streak, always eager for the next game. 
   Looking back, Fluffy's life has been one of consistent love and attention—a 
   testament to the bond between pet and owner.
   
   Their personality today: Cheerful, Playful, Grateful, Resilient
   Total age: 487 ticks (approximately 48 days)
   ═══════════════════════════════════════════════
   ```

5. **Achievement Integration**
   - Unlock "Biographer" achievement after generating first story
   - Unlock "Epic Chronicler" after pet survives 1000+ ticks with story generated

6. **Auto-Generation Triggers**
   ```java
   @EventHandler
   void on(PetEvolvedEvent event) {
     // Generate new story on evolution milestones
     storyService.scheduleStoryGeneration(event.petId());
   }
   
   @EventHandler
   void on(PetDiedEvent event) {
     // Generate epitaph/memorial story
     storyService.generateMemorialStory(event.petId());
   }
   ```

### Technical Notes
- Stories are expensive to generate - cache aggressively
- Only regenerate when significant events occur (evolution, death)
- Consider using cheaper LLM model for stories (doesn't need latest model)
- Store stories as separate projection for fast retrieval

---

## Integration Architecture Summary

### Event Flow with LLM Integration
```
User Input (Natural Language)
     │
     ▼
┌──────────────────────────┐
│  Intent Recognition      │──► LLM API Call (async)
│  Service                 │
└──────────────────────────┘
     │
     ▼ (Parsed Intent)
┌──────────────────────────┐
│  Command Gateway         │──► Dispatch to Aggregate
└──────────────────────────┘
     │
     ▼ (Command)
┌──────────────────────────┐
│  Pet Aggregate           │──► Emit Event
└──────────────────────────┘
     │
     ▼ (Event)
┌──────────────────────────┐
│  Personality Saga        │──► Analyze & Update Personality
│  (Triggers periodically) │
└──────────────────────────┘
     │
     ▼ (Personality Updated)
┌──────────────────────────┐
│  LLM Response Service    │──► Generate contextual responses
│  (Uses updated context)  │
└──────────────────────────┘
```

### Reactor Patterns for LLM Calls

1. **Async with Timeout**
   ```java
   Mono<String> llmCall = llmService.generateResponse(prompt)
     .timeout(Duration.ofSeconds(10))
     .onErrorResume(TimeoutException.class, e -> 
       Mono.just("*yawns* Sorry, I'm a bit slow today..."))
     .onErrorResume(e -> 
       Mono.just("*confused look* I didn't catch that."));
   ```

2. **Retry with Backoff**
   ```java
   Mono<String> llmCall = llmService.generateResponse(prompt)
     .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
       .maxBackoff(Duration.ofSeconds(10))
       .filter(throwable -> throwable instanceof WebClientException));
   ```

3. **Parallel Calls with Combine**
   ```java
   Mono<ChatContext> context = Mono.zip(
     queryGateway.query(new GetPetStatusQuery(petId), PetStatus.class),
     queryGateway.query(new GetPersonalityQuery(petId), PetPersonality.class),
     queryGateway.query(new GetPetMemoriesQuery(petId), MemoryList.class)
   ).map(tuple -> new ChatContext(tuple.getT1(), tuple.getT2(), tuple.getT3()));
   ```

4. **Caching with Conditional Fetch**
   ```java
   Mono<String> cachedOrFetch = 
     Mono.justOrEmpty(responseCache.get(cacheKey))
       .switchIfEmpty(llmService.generateResponse(prompt)
         .doOnNext(response -> responseCache.put(cacheKey, response)));
   ```

---

## Configuration & Dependencies

### Additional Dependencies
```gradle
// LLM Integration
implementation 'org.springframework.boot:spring-boot-starter-webflux'
implementation 'io.projectreactor:reactor-core'
implementation 'io.projectreactor.netty:reactor-netty'

// JSON Processing for LLM responses
implementation 'com.fasterxml.jackson.core:jackson-databind'

// Rate Limiting (optional)
implementation 'io.github.bucket4j:bucket4j-core:8.1.0'

// Caching
implementation 'org.springframework.boot:spring-boot-starter-cache'
implementation 'com.github.ben-manes.caffeine:caffeine'
```

### Application Configuration
```yaml
llm:
  provider: anthropic  # or openai
  api-key: ${LLM_API_KEY}
  model: claude-sonnet-4-20250514  # or gpt-4
  timeout: 10s
  max-tokens: 500
  temperature: 0.7
  
  rate-limit:
    enabled: true
    requests-per-minute: 20
    
  caching:
    enabled: true
    ttl: 1h
    max-size: 1000

pet-conversation:
  personality-update-interval: 50  # ticks
  chat-history-limit: 10
  emotional-game-cooldown: 50  # ticks
```

### Environment Variables
```bash
# .env file
LLM_API_KEY=your_api_key_here
LLM_PROVIDER=anthropic  # or openai
```

---

## Testing Strategy

### Unit Tests
```java
@Test
void testPersonalityEvolution_WellCaredPet_BecomesCheerful() {
  fixture.given(
    new PetCreatedEvent("pet1", "Fluffy", DRAGON, now()),
    // ... 50 feeding and playing events
  )
  .when(new UpdatePersonalityCommand("pet1"))
  .expectEvents(
    new PersonalityUpdatedEvent("pet1", Set.of(CHEERFUL, PLAYFUL))
  );
}
```

### Integration Tests with Mocked LLM
```java
@Test
void testChatFlow_WithMockedLLM() {
  when(llmService.generateResponse(any()))
    .thenReturn(Mono.just("I'm doing great! Thanks for asking!"));
    
  String response = conversationService.chat("pet1", "How are you?").block();
  
  assertThat(response).contains("doing great");
  verify(queryGateway).query(any(GetPetStatusQuery.class));
}
```

### LLM Integration Tests (Manual/E2E)
```java
@Test
@EnabledIfEnvironmentVariable(named = "LLM_API_KEY", matches = ".+")
void testRealLLMIntegration() {
  // Only runs if API key is set
  String response = llmService.generateResponse(
    "You are a happy dragon. Respond to: Hello!"
  ).block();
  
  assertThat(response).isNotNull();
  assertThat(response.length()).isGreaterThan(0);
}
```

### Reactor Testing
```java
@Test
void testLLMTimeout() {
  StepVerifier.create(
    llmService.generateResponse("test")
      .timeout(Duration.ofMillis(100))
  )
  .expectError(TimeoutException.class)
  .verify();
}
```

---

## Cost Management

### LLM API Cost Estimates
```
Anthropic Claude Sonnet 4:
- Input: $3.00 / 1M tokens
- Output: $15.00 / 1M tokens

Per chat interaction (~500 input + 100 output tokens):
- Cost: ~$0.0015 per interaction

For 1000 pets with average 10 chats/day:
- Daily cost: ~$150
- Monthly cost: ~$4,500

Mitigation strategies:
1. Aggressive caching (same context = cached response)
2. Rate limiting per pet (max 1 chat per 5 seconds)
3. Use cheaper models for non-critical tasks (stories)
4. Batch personality updates (once per 50 ticks, not every tick)
```

### Cost Optimization Techniques

1. **Response Caching**
   ```java
   String cacheKey = petId + "_" + 
     hash(status, personality, userMessage);
   
   if (cache.contains(cacheKey)) {
     return cache.get(cacheKey); // No LLM call
   }
   ```

2. **Tier System**
   - Free tier: 10 LLM chats per day per user
   - Premium: Unlimited chats, priority responses
   - Use local fallbacks for free tier excess

3. **Model Selection**
   ```java
   LLMModel selectModel(TaskType task) {
     return switch(task) {
       case CHAT -> CLAUDE_SONNET; // Best quality
       case STORY -> CLAUDE_HAIKU; // Cheaper, still good
       case INTENT -> GPT_3_5; // Fast and cheap
     };
   }
   ```

4. **Prompt Compression**
   - Minimize token count in prompts
   - Use abbreviations in system prompts
   - Remove unnecessary context

---

## Future Enhancements

### Advanced LLM Features
- **Voice Synthesis:** Convert LLM text responses to speech
- **Multi-Pet Conversations:** Pets interact with each other via LLM
- **Dream Sequences:** Generate random "dreams" based on personality
- **Pet Advice:** Pet gives advice to owner (role reversal)

### Personality Evolution
- **Trauma System:** Negative events create lasting personality impacts
- **Attachment Styles:** Secure, anxious, avoidant based on care consistency
- **Memory Consolidation:** Older memories fade, recent ones emphasized
- **Personality Regression:** Neglected adult pets revert to anxious traits

### Multi-Modal
- **Image Understanding:** Upload pet photos, LLM describes them in-character
- **Emotion Detection:** Analyze user's text sentiment, pet responds empathetically
- **Visual Storytelling:** Generate images to accompany life stories (DALL-E integration)

---

## Key Learning Outcomes (LLM Integration)

By completing these phases, you will gain hands-on experience with:

✅ **External API Integration:** Async HTTP calls with WebClient and Reactor  
✅ **Non-Deterministic Operations:** Handling LLM unpredictability in event-sourced systems  
✅ **Saga Coordination:** Managing multi-step workflows with external dependencies  
✅ **Context Enrichment:** Building rich prompts from event streams and projections  
✅ **Error Handling:** Timeouts, retries, fallbacks for external services  
✅ **Caching Strategies:** Reducing API costs through intelligent caching  
✅ **Reactor Patterns:** `Mono`, `Flux`, `zip`, `switchIfEmpty`, `onErrorResume`  
✅ **Prompt Engineering:** Crafting effective LLM prompts for consistent outputs  
✅ **Cost Management:** Balancing quality and expense in production systems  
✅ **Testing External Services:** Mocking, test doubles, conditional integration tests

---

## Implementation Order Recommendation

For Claude Code execution, implement in this order:

1. **Phase 10** - Foundation: Personality system without LLM (logic-based)
2. **Phase 11** - Core LLM: Chat interface with basic responses
3. **Phase 13** - NLU: Natural language command parsing
4. **Phase 12** - Game: Emotional intelligence mini-game
5. **Phase 14** - Storytelling: Life story generation

This order ensures:
- Basic personality tracking works before adding LLM complexity
- LLM integration tested with simple chat before complex games
- Natural language parsing reuses chat infrastructure
- Games and stories are "bonus" features that don't block core functionality

---

## Troubleshooting Guide

### Common Issues

**Issue:** LLM responses are slow or timing out  
**Solution:** Increase timeout, implement caching, use faster model

**Issue:** LLM returns non-JSON or malformed responses  
**Solution:** Add JSON validation, retry with corrected prompt, fallback to default

**Issue:** Personality updates causing event storm  
**Solution:** Reduce update frequency, batch updates, add cooldown period

**Issue:** High API costs  
**Solution:** Enable aggressive caching, implement rate limiting, use tiered system

**Issue:** Chat responses inconsistent with pet state  
**Solution:** Verify query results are being passed to LLM, check prompt template

**Issue:** Saga not triggering LLM calls  
**Solution:** Verify saga association property matches event, check saga lifecycle

---

## Conclusion

The LLM integration transforms your virtual pet from a state machine into a dynamic, personality-driven companion. The event-sourced architecture ensures that every interaction contributes to the pet's evolving character, while Reactor patterns keep external API calls non-blocking and resilient.

This bounded context demonstrates production-ready patterns for integrating AI services into event-driven systems—skills directly applicable to modern software architecture.

Ready to give your pets a voice? Start with Phase 10 and watch them come alive! 🐉✨