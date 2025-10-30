package com.reactor.pets.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;

@Entity
@Table(name = "global_time_view")
@Data
public class GlobalTimeView {
  @Id
  private String timeId;
  private long currentGlobalTick;
  private Instant lastUpdated;
}
