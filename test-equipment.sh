#!/bin/bash

# Test script for equipment system
BASE_URL="http://localhost:8080/api"

echo "=== Equipment System Test ==="
echo

# 1. Create a new pet that can use equipment (need to get it past EGG stage)
echo "1. Creating a new pet..."
PET_RESPONSE=$(curl -s -X POST $BASE_URL/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Equipment Test","type":"DOG"}')

PET_ID=$(echo $PET_RESPONSE | jq -r '.petId')
echo "Created pet: $PET_ID"
echo "Pet stage: $(echo $PET_RESPONSE | jq -r '.stage')"
echo "Pet slots: $(echo $PET_RESPONSE | jq -r '.maxEquipmentSlots // 0')"
echo

# 2. Check inventory
echo "2. Checking inventory..."
INVENTORY=$(curl -s $BASE_URL/inventory/equipment)
if [ -z "$INVENTORY" ]; then
  echo "❌ Inventory not found (404)"
else
  echo "✓ Inventory found:"
  echo $INVENTORY | jq .
fi
echo

# 3. Check pet equipment
echo "3. Checking pet equipment..."
curl -s $BASE_URL/pets/$PET_ID/equipment | jq .
echo

echo "=== Test Complete ==="
echo
echo "Note: Pet is still an EGG (maxEquipmentSlots=0)"
echo "To test equipping, the pet needs to evolve to BABY stage first."
