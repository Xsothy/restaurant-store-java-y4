#!/bin/bash

# Test script for updating driver location
# This simulates what the Admin Backend would do when updating driver location

# Configuration
STORE_API="http://localhost:8080"
ORDER_ID="2"  # Change this to match an existing order ID

# Test coordinates (Phnom Penh area)
LAT="11.5564"
LNG="104.9282"

echo "Testing Driver Location Update"
echo "=============================="
echo "Order ID: $ORDER_ID"
echo "Latitude: $LAT"
echo "Longitude: $LNG"
echo ""

# Test 1: Update location with coordinates
echo "Test 1: Updating location with coordinates..."
RESPONSE=$(curl -s -X POST "${STORE_API}/api/deliveries/${ORDER_ID}/location?latitude=${LAT}&longitude=${LNG}")
echo "Response: $RESPONSE"
echo ""

# Test 2: Get delivery to verify update
echo "Test 2: Fetching delivery information..."
TOKEN=$(curl -s -X POST "${STORE_API}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@resstore.com","password":"password123"}' | jq -r '.data.token')

if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
  DELIVERY=$(curl -s -X GET "${STORE_API}/api/deliveries/${ORDER_ID}" \
    -H "Authorization: Bearer $TOKEN")
  echo "Delivery: $DELIVERY" | jq '.'
else
  echo "Failed to authenticate. Please check credentials."
fi

echo ""
echo "Test complete!"
echo ""
echo "To view the map:"
echo "1. Open browser to http://localhost:8080/orders/${ORDER_ID}"
echo "2. Login with customer credentials"
echo "3. The map should appear showing driver location at ${LAT}, ${LNG}"
