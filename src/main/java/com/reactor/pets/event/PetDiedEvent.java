package com.reactor.pets.event;

import com.reactor.pets.domain.EquipmentItem;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class PetDiedEvent {
  String petId;
  int finalAge;
  long localAge; // Ticks since pet was born
  String causeOfDeath;
  List<EquipmentItem> equippedItems; // Items that were equipped when pet died
  Instant timestamp;
}
