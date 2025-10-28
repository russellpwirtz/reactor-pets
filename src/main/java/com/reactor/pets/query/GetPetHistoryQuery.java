package com.reactor.pets.query;

import lombok.Value;

@Value
public class GetPetHistoryQuery {
  String petId;
  int limit; // Maximum number of events to return (default 10, max 50)
}
