package com.reactor.pets.query;

import lombok.Value;

/**
 * Query to get a specific shop item by ID.
 */
@Value
public class GetShopItemQuery {
  String itemId;
}
