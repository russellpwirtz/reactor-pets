package com.reactor.pets.aggregate;

import com.reactor.pets.command.CleanPetCommand;
import com.reactor.pets.command.CreatePetCommand;
import com.reactor.pets.command.EquipItemCommand;
import com.reactor.pets.command.EvolvePetCommand;
import com.reactor.pets.command.FeedPetCommand;
import com.reactor.pets.command.MournPetCommand;
import com.reactor.pets.command.PlayWithPetCommand;
import com.reactor.pets.command.RequestEquipItemCommand;
import com.reactor.pets.command.RequestUnequipItemCommand;
import com.reactor.pets.command.RequestUseConsumableCommand;
import com.reactor.pets.command.TimeTickCommand;
import com.reactor.pets.command.UnequipItemCommand;
import com.reactor.pets.command.UseConsumableCommand;
import com.reactor.pets.domain.Consumable;
import com.reactor.pets.domain.ConsumableCatalog;
import com.reactor.pets.domain.EquipmentItem;
import com.reactor.pets.domain.EquipmentSlot;
import com.reactor.pets.domain.StatModifier;
import com.reactor.pets.event.ConsumableUseRequestedEvent;
import com.reactor.pets.event.ConsumableUsedEvent;
import com.reactor.pets.event.ItemEquipRequestedEvent;
import com.reactor.pets.event.ItemEquippedEvent;
import com.reactor.pets.event.ItemUnequipRequestedEvent;
import com.reactor.pets.event.ItemUnequippedEvent;
import com.reactor.pets.event.PetBecameSickEvent;
import com.reactor.pets.event.PetCleanedEvent;
import com.reactor.pets.event.PetCreatedEvent;
import com.reactor.pets.event.PetCuredEvent;
import com.reactor.pets.event.PetDiedEvent;
import com.reactor.pets.event.PetEvolvedEvent;
import com.reactor.pets.event.PetFedEvent;
import com.reactor.pets.event.PetHealthDeterioratedEvent;
import com.reactor.pets.event.PetMournedEvent;
import com.reactor.pets.event.PetPlayedWithEvent;
import com.reactor.pets.event.TimePassedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor
public class Pet {

  @AggregateIdentifier
  private String petId;
  private String name;
  private PetType type;
  private int hunger; // 0-100
  private int happiness; // 0-100
  private int health; // 0-100
  private PetStage stage;
  private EvolutionPath evolutionPath;
  private boolean isAlive;
  private int age; // Age in units (every 10 ticks = 1 age)
  private int totalTicks; // Total time ticks elapsed
  private long lastTickSequence; // Last processed tick sequence (for idempotency)
  private double xpMultiplier; // Multiplier for XP earned from/by this pet (starts at 1.0)

  // Equipment system fields
  private Map<EquipmentSlot, EquipmentItem> equippedItems; // Items currently equipped
  private int maxEquipmentSlots; // Number of slots available (based on stage)

  // Sickness system fields
  private boolean isSick; // Whether the pet is currently sick
  private int lowHealthTicks; // Number of consecutive ticks with health < 30

  @CommandHandler
  public Pet(CreatePetCommand command) {
    // Validate command
    if (command.getName() == null || command.getName().isBlank()) {
      throw new IllegalArgumentException("Pet name cannot be empty");
    }
    if (command.getType() == null) {
      throw new IllegalArgumentException("Pet type cannot be null");
    }

    // Apply event
    AggregateLifecycle.apply(
        new PetCreatedEvent(
            command.getPetId(), command.getName(), command.getType(), Instant.now()));
  }

  @CommandHandler
  public void handle(FeedPetCommand command) {
    // Business rules validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot feed a dead pet");
    }
    if (command.getFoodAmount() <= 0) {
      throw new IllegalArgumentException("Food amount must be positive");
    }

    // Calculate hunger reduction with equipment modifiers
    double foodEfficiency = 1.0 + getTotalModifier(StatModifier.FOOD_EFFICIENCY);
    int baseReduction = command.getFoodAmount();
    int modifiedReduction = (int) Math.ceil(baseReduction * foodEfficiency);
    int hungerReduction = Math.min(modifiedReduction, this.hunger);

    // Apply event
    AggregateLifecycle.apply(new PetFedEvent(command.getPetId(), hungerReduction, Instant.now()));
  }

  @CommandHandler
  public void handle(PlayWithPetCommand command) {
    // Business rules validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot play with a dead pet");
    }
    if (this.happiness >= 100) {
      throw new IllegalStateException("Pet is already at maximum happiness");
    }

    // Playing increases happiness with equipment modifiers (+15 base)
    double playEfficiency = 1.0 + getTotalModifier(StatModifier.PLAY_EFFICIENCY);
    int baseHappinessIncrease = 15;
    int modifiedHappinessIncrease = (int) Math.ceil(baseHappinessIncrease * playEfficiency);
    int happinessIncrease = Math.min(modifiedHappinessIncrease, 100 - this.happiness);

    // Playing also increases hunger (+5)
    int hungerIncrease = Math.min(5, 100 - this.hunger);

    // Apply event
    AggregateLifecycle.apply(
        new PetPlayedWithEvent(
            command.getPetId(), happinessIncrease, hungerIncrease, Instant.now()));
  }

  @CommandHandler
  public void handle(CleanPetCommand command) {
    // Business rules validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot clean a dead pet");
    }

    // Cleaning increases health (+10), capped at 100
    // Unlike playing, cleaning is allowed even at max health (it just won't increase health)
    int healthIncrease = Math.min(10, 100 - this.health);

    // Apply event
    AggregateLifecycle.apply(
        new PetCleanedEvent(command.getPetId(), healthIncrease, Instant.now()));
  }

  @CommandHandler
  public void handle(RequestUseConsumableCommand command) {
    // Validate pet state only - saga will validate inventory
    if (!isAlive) {
      throw new IllegalStateException("Cannot use consumable on dead pet");
    }

    if (command.getConsumableType() == null) {
      throw new IllegalArgumentException("Consumable type cannot be null");
    }

    // Sick pets cannot use toys
    if (this.isSick && command.getConsumableType() == com.reactor.pets.domain.ConsumableType.PREMIUM_TOY) {
      throw new IllegalStateException("Sick pets cannot use toys");
    }

    // Emit request event (saga will validate inventory and apply effects)
    AggregateLifecycle.apply(
        new ConsumableUseRequestedEvent(
            command.getPetId(),
            command.getPlayerId(),
            command.getConsumableType(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(UseConsumableCommand command) {
    // Business rules validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot use consumable on a dead pet");
    }
    if (command.getConsumableType() == null) {
      throw new IllegalArgumentException("Consumable type cannot be null");
    }

    // Sick pets cannot play, but can use consumables
    if (this.isSick && command.getConsumableType() == com.reactor.pets.domain.ConsumableType.PREMIUM_TOY) {
      throw new IllegalStateException("Sick pets cannot use toys");
    }

    // Look up consumable in catalog
    Consumable consumable = ConsumableCatalog.getConsumable(command.getConsumableType())
        .orElseThrow(() -> new IllegalArgumentException("Unknown consumable type: " + command.getConsumableType()));

    // Apply equipment modifiers to consumable effects
    double foodEfficiency = 1.0 + getTotalModifier(StatModifier.FOOD_EFFICIENCY);
    double playEfficiency = 1.0 + getTotalModifier(StatModifier.PLAY_EFFICIENCY);

    // Calculate actual restoration amounts with equipment modifiers
    int hungerRestored = (int) Math.ceil(consumable.getHungerRestore() * foodEfficiency);
    hungerRestored = Math.min(hungerRestored, this.hunger);

    int happinessRestored = (int) Math.ceil(consumable.getHappinessRestore() * playEfficiency);
    happinessRestored = Math.min(happinessRestored, 100 - this.happiness);

    int healthRestored = consumable.getHealthRestore();
    healthRestored = Math.min(healthRestored, 100 - this.health);

    boolean curedSickness = consumable.isCuresSickness() && this.isSick;

    // Apply event
    AggregateLifecycle.apply(
        new ConsumableUsedEvent(
            command.getPetId(),
            command.getConsumableType(),
            hungerRestored,
            happinessRestored,
            healthRestored,
            curedSickness,
            Instant.now()));
  }

  @CommandHandler
  public void handle(EvolvePetCommand command) {
    // Business rules validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot evolve a dead pet");
    }
    if (command.getNewStage() == null) {
      throw new IllegalArgumentException("New stage cannot be null");
    }
    if (command.getNewStage().ordinal() <= this.stage.ordinal()) {
      throw new IllegalStateException(
          "Cannot evolve to a stage lower than or equal to current stage");
    }

    // Apply event
    AggregateLifecycle.apply(
        new PetEvolvedEvent(
            command.getPetId(),
            this.stage,
            command.getNewStage(),
            command.getEvolutionPath(),
            command.getEvolutionReason(),
            Instant.now()));
  }

  @EventSourcingHandler
  public void on(PetCreatedEvent event) {
    this.petId = event.getPetId();
    this.name = event.getName();
    this.type = event.getType();
    this.hunger = 30;
    this.happiness = 70;
    this.health = 100;
    this.stage = PetStage.EGG;
    this.evolutionPath = EvolutionPath.UNDETERMINED;
    this.isAlive = true;
    this.age = 0;
    this.totalTicks = 0;
    this.lastTickSequence = -1;
    this.xpMultiplier = 1.0; // Start with 1.0x multiplier
    this.equippedItems = new HashMap<>();
    this.maxEquipmentSlots = 0; // Eggs have no equipment slots
    this.isSick = false;
    this.lowHealthTicks = 0;
  }

  @EventSourcingHandler
  public void on(PetFedEvent event) {
    this.hunger = Math.max(0, this.hunger - event.getHungerReduction());
  }

  @EventSourcingHandler
  public void on(PetPlayedWithEvent event) {
    this.happiness = Math.min(100, this.happiness + event.getHappinessIncrease());
    this.hunger = Math.min(100, this.hunger + event.getHungerIncrease());
  }

  @EventSourcingHandler
  public void on(PetCleanedEvent event) {
    this.health = Math.min(100, this.health + event.getHealthIncrease());
  }

  @EventSourcingHandler
  public void on(ConsumableUsedEvent event) {
    // Apply consumable effects
    this.hunger = Math.max(0, this.hunger - event.getHungerRestored());
    this.happiness = Math.min(100, this.happiness + event.getHappinessRestored());
    this.health = Math.min(100, this.health + event.getHealthRestored());

    // Cure sickness if consumable has that effect
    if (event.isCuredSickness()) {
      this.isSick = false;
      this.lowHealthTicks = 0;
    }
  }

  @EventSourcingHandler
  public void on(PetBecameSickEvent event) {
    this.isSick = true;
  }

  @EventSourcingHandler
  public void on(PetCuredEvent event) {
    this.isSick = false;
    this.lowHealthTicks = 0;
  }

  @EventSourcingHandler
  public void on(PetEvolvedEvent event) {
    this.stage = event.getNewStage();
    this.evolutionPath = event.getEvolutionPath();

    // Update equipment slots based on stage: Baby=1, Teen=2, Adult=3
    switch (event.getNewStage()) {
      case BABY:
        this.maxEquipmentSlots = 1;
        break;
      case TEEN:
        this.maxEquipmentSlots = 2;
        break;
      case ADULT:
        this.maxEquipmentSlots = 3;
        break;
      default:
        this.maxEquipmentSlots = 0; // EGG has 0 slots
        break;
    }
  }

  @CommandHandler
  public void handle(TimeTickCommand command) {
    // Ignore if pet is dead
    if (!isAlive) {
      return; // No-op for dead pets
    }

    // Idempotency check: ignore if we've already processed this tick
    if (command.getTickCount() <= lastTickSequence) {
      return;
    }

    // Calculate stat changes based on stage and evolution path
    int baseHungerIncrease = 3;
    int baseHappinessDecrease = 2;

    // Adults have slower degradation
    if (this.stage == PetStage.ADULT) {
      baseHungerIncrease = 2;
      baseHappinessDecrease = 1;
    }

    // Neglected path has faster degradation (50% increase)
    if (this.evolutionPath == EvolutionPath.NEGLECTED) {
      baseHungerIncrease = (int) Math.ceil(baseHungerIncrease * 1.5);
      baseHappinessDecrease = (int) Math.ceil(baseHappinessDecrease * 1.5);
    }

    // Apply equipment modifiers to decay rates
    double hungerDecayModifier = 1.0 + getTotalModifier(StatModifier.HUNGER_DECAY_RATE);
    double happinessDecayModifier = 1.0 + getTotalModifier(StatModifier.HAPPINESS_DECAY_RATE);

    baseHungerIncrease = (int) Math.ceil(baseHungerIncrease * hungerDecayModifier);
    baseHappinessDecrease = (int) Math.ceil(baseHappinessDecrease * happinessDecayModifier);

    int hungerIncrease = Math.min(baseHungerIncrease, 100 - this.hunger);
    int happinessDecrease = Math.min(baseHappinessDecrease, this.happiness);

    // Every 10 ticks = 1 age unit
    int ageIncrease = ((totalTicks + 1) % 10 == 0) ? 1 : 0;

    // Calculate XP multiplier change
    // Increases by +0.1x every 50 ticks
    double xpMultiplierChange = 0.0;
    if ((totalTicks + 1) % 50 == 0) {
      xpMultiplierChange = 0.1;
    }

    // Check for care quality bonus: +0.05x if all stats >70 for this tick
    // (Simplified: check current stats after this tick's changes)
    int futureHunger = Math.min(100, this.hunger + hungerIncrease);
    int futureHappiness = Math.max(0, this.happiness - happinessDecrease);
    if (futureHunger <= 30 && futureHappiness >= 70 && this.health >= 70) {
      xpMultiplierChange += 0.05;
    }

    double newXpMultiplier = this.xpMultiplier + xpMultiplierChange;

    // Apply time passed event
    AggregateLifecycle.apply(
        new TimePassedEvent(
            command.getPetId(),
            hungerIncrease,
            happinessDecrease,
            ageIncrease,
            command.getTickCount(),
            xpMultiplierChange,
            newXpMultiplier,
            Instant.now()));

    // Apply health regeneration from equipment (if any)
    double healthRegen = getTotalModifier(StatModifier.HEALTH_REGEN);
    if (healthRegen > 0) {
      int healthIncrease = (int) Math.ceil(healthRegen);
      healthIncrease = Math.min(healthIncrease, 100 - this.health);
      if (healthIncrease > 0) {
        AggregateLifecycle.apply(
            new PetCleanedEvent(command.getPetId(), healthIncrease, Instant.now()));
      }
    }

    // Calculate health deterioration after time has passed
    int newHunger = Math.min(100, this.hunger + hungerIncrease);
    int newHappiness = Math.max(0, this.happiness - happinessDecrease);
    int healthDecrease = 0;
    String deteriorationReason = null;

    if (newHunger > 80) {
      healthDecrease += 5;
      deteriorationReason = "Extreme hunger";
    }
    if (newHappiness < 20) {
      healthDecrease += 3;
      if (deteriorationReason != null) {
        deteriorationReason += " and low happiness";
      } else {
        deteriorationReason = "Low happiness";
      }
    }

    // Apply health deterioration if needed
    if (healthDecrease > 0) {
      healthDecrease = Math.min(healthDecrease, this.health);
      AggregateLifecycle.apply(
          new PetHealthDeterioratedEvent(
              command.getPetId(), healthDecrease, deteriorationReason, Instant.now()));
    }

    // Check for death after health deterioration
    int newHealth = this.health - healthDecrease;
    if (newHealth <= 0) {
      // Collect equipped items to return to inventory (use ArrayList for Axon serialization)
      var equippedItemsList = new ArrayList<>(this.equippedItems.values());

      AggregateLifecycle.apply(
          new PetDiedEvent(
              command.getPetId(),
              this.age + ageIncrease,
              this.totalTicks + 1,
              "Health reached zero: " + deteriorationReason,
              equippedItemsList,
              Instant.now()));
    }
  }

  @EventSourcingHandler
  public void on(TimePassedEvent event) {
    this.hunger = Math.min(100, this.hunger + event.getHungerIncrease());
    this.happiness = Math.max(0, this.happiness - event.getHappinessDecrease());
    this.age += event.getAgeIncrease();
    this.totalTicks++;
    this.lastTickSequence = event.getTickCount();
    this.xpMultiplier = event.getNewXpMultiplier();
  }

  @EventSourcingHandler
  public void on(PetHealthDeterioratedEvent event) {
    this.health = Math.max(0, this.health - event.getHealthDecrease());
  }

  @EventSourcingHandler
  public void on(PetDiedEvent event) {
    this.isAlive = false;
  }

  @CommandHandler
  public void handle(MournPetCommand command) {
    // Only alive pets can mourn
    if (!isAlive) {
      return; // Silently ignore for dead pets
    }

    // Apply mourning event
    AggregateLifecycle.apply(
        new PetMournedEvent(
            this.petId,
            command.getDeceasedPetId(),
            command.getHappinessLoss(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(RequestEquipItemCommand command) {
    // Validate pet state
    if (!isAlive) {
      throw new IllegalStateException("Cannot equip items to dead pet");
    }

    if (command.getSlot() == null) {
      throw new IllegalArgumentException("Equipment slot cannot be null");
    }

    // Validate that this is not an egg (eggs have 0 slots)
    if (maxEquipmentSlots == 0) {
      throw new IllegalStateException(
          "Cannot equip items to pet in stage " + this.stage);
    }

    // Check if slot is already occupied and if we have room
    int equippedCount = equippedItems.size();
    boolean isReplacing = equippedItems.containsKey(command.getSlot());

    if (!isReplacing && equippedCount >= maxEquipmentSlots) {
      throw new IllegalStateException(
          "Cannot equip more items. Pet has " + maxEquipmentSlots + " slots and " + equippedCount + " items equipped");
    }

    // Emit request event (saga will coordinate inventory check)
    AggregateLifecycle.apply(
        new ItemEquipRequestedEvent(
            command.getPetId(),
            command.getPlayerId(),
            command.getItemId(),
            command.getSlot(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(RequestUnequipItemCommand command) {
    // Validate pet state
    if (!isAlive) {
      throw new IllegalStateException("Cannot unequip items from dead pet");
    }

    if (command.getSlot() == null) {
      throw new IllegalArgumentException("Equipment slot cannot be null");
    }

    if (!equippedItems.containsKey(command.getSlot())) {
      throw new IllegalStateException("No item equipped in slot " + command.getSlot());
    }

    // Emit request event (saga will coordinate returning to inventory)
    AggregateLifecycle.apply(
        new ItemUnequipRequestedEvent(
            command.getPetId(),
            command.getPlayerId(),
            command.getSlot(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(EquipItemCommand command) {
    // Validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot equip items to a dead pet");
    }
    if (command.getItem() == null) {
      throw new IllegalArgumentException("Item cannot be null");
    }
    if (command.getSlot() == null) {
      throw new IllegalArgumentException("Slot cannot be null");
    }
    if (command.getItem().getSlot() != command.getSlot()) {
      throw new IllegalArgumentException(
          "Item slot type " + command.getItem().getSlot() + " does not match target slot " + command.getSlot());
    }

    // Count currently equipped items
    int equippedCount = equippedItems.size();
    boolean isReplacing = equippedItems.containsKey(command.getSlot());

    if (!isReplacing && equippedCount >= maxEquipmentSlots) {
      throw new IllegalStateException(
          "Cannot equip more items. Pet has " + maxEquipmentSlots + " slots and " + equippedCount + " items equipped");
    }

    // Apply event
    AggregateLifecycle.apply(
        new ItemEquippedEvent(
            this.petId,
            command.getItem(),
            command.getSlot(),
            Instant.now()));
  }

  @CommandHandler
  public void handle(UnequipItemCommand command) {
    // Validation
    if (!isAlive) {
      throw new IllegalStateException("Cannot unequip items from a dead pet");
    }
    if (command.getSlot() == null) {
      throw new IllegalArgumentException("Slot cannot be null");
    }
    if (!equippedItems.containsKey(command.getSlot())) {
      throw new IllegalStateException("No item equipped in slot " + command.getSlot());
    }

    // Get the item to unequip
    EquipmentItem item = equippedItems.get(command.getSlot());

    // Apply event
    AggregateLifecycle.apply(
        new ItemUnequippedEvent(
            this.petId,
            item,
            command.getSlot(),
            Instant.now()));
  }

  @EventSourcingHandler
  public void on(ItemEquippedEvent event) {
    this.equippedItems.put(event.getSlot(), event.getItem());
  }

  @EventSourcingHandler
  public void on(ItemUnequippedEvent event) {
    this.equippedItems.remove(event.getSlot());
  }

  @EventSourcingHandler
  public void on(PetMournedEvent event) {
    this.happiness = Math.max(0, this.happiness - event.getHappinessLoss());
  }

  /**
   * Gets the total modifier value for a given stat from all equipped items.
   * Returns 0.0 if no modifiers are present for that stat.
   */
  private double getTotalModifier(StatModifier modifier) {
    return equippedItems.values().stream()
        .mapToDouble(item -> item.getModifier(modifier))
        .sum();
  }
}
