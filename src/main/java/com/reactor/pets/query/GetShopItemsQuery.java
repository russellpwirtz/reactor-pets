package com.reactor.pets.query;

import com.reactor.pets.domain.ItemType;
import lombok.Value;

/**
 * Query to get all shop items, optionally filtered by type.
 */
@Value
public class GetShopItemsQuery {
  ItemType itemType; // null to get all items
}
