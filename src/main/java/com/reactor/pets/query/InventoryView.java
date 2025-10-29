package com.reactor.pets.query;

import com.reactor.pets.domain.EquipmentItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Read model for player inventory.
 */
@Entity
@Table(name = "inventory_view")
@Data
public class InventoryView {

  @Id
  @Column(name = "player_id")
  private String playerId;

  @Column(name = "items", columnDefinition = "TEXT")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<EquipmentItem> items = new ArrayList<>();

  @Column(name = "last_updated")
  private Instant lastUpdated;
}
