#!/bin/bash

# Test Admin DTOs - Verify structure and compilation

echo "=================================="
echo "Admin DTOs Verification Test"
echo "=================================="
echo ""

# Test 1: Verify all DTOs compile
echo "Test 1: Verifying compilation..."
cd /home/engine/project
mvn clean compile -DskipTests -q
if [ $? -eq 0 ]; then
    echo "✅ All DTOs compiled successfully"
else
    echo "❌ Compilation failed"
    exit 1
fi
echo ""

# Test 2: Check for any remaining references to admin project
echo "Test 2: Checking for invalid external references..."
ADMIN_REFS=$(grep -r "com\.resadmin" src/main/java/com/restaurant/store/dto/admin/ 2>/dev/null | wc -l)
if [ $ADMIN_REFS -eq 0 ]; then
    echo "✅ No references to external admin project found"
else
    echo "❌ Found $ADMIN_REFS references to com.resadmin package"
    grep -r "com\.resadmin" src/main/java/com/restaurant/store/dto/admin/
    exit 1
fi
echo ""

# Test 3: Verify package declarations
echo "Test 3: Verifying package declarations..."
WRONG_PACKAGES=0

# Check main DTOs
for file in src/main/java/com/restaurant/store/dto/admin/*.java; do
    if [ -f "$file" ]; then
        PKG=$(grep "^package" "$file" | grep -v "com.restaurant.store.dto.admin;$")
        if [ -n "$PKG" ]; then
            echo "❌ Wrong package in $file: $PKG"
            WRONG_PACKAGES=$((WRONG_PACKAGES + 1))
        fi
    fi
done

# Check request DTOs
for file in src/main/java/com/restaurant/store/dto/admin/request/*.java; do
    if [ -f "$file" ]; then
        PKG=$(grep "^package" "$file" | grep -v "com.restaurant.store.dto.admin.request;$")
        if [ -n "$PKG" ]; then
            echo "❌ Wrong package in $file: $PKG"
            WRONG_PACKAGES=$((WRONG_PACKAGES + 1))
        fi
    fi
done

# Check response DTOs
for file in src/main/java/com/restaurant/store/dto/admin/response/*.java; do
    if [ -f "$file" ]; then
        PKG=$(grep "^package" "$file" | grep -v "com.restaurant.store.dto.admin.response;$")
        if [ -n "$PKG" ]; then
            echo "❌ Wrong package in $file: $PKG"
            WRONG_PACKAGES=$((WRONG_PACKAGES + 1))
        fi
    fi
done

# Check websocket DTOs
for file in src/main/java/com/restaurant/store/dto/admin/websocket/*.java; do
    if [ -f "$file" ]; then
        PKG=$(grep "^package" "$file" | grep -v "com.restaurant.store.dto.admin.websocket;$")
        if [ -n "$PKG" ]; then
            echo "❌ Wrong package in $file: $PKG"
            WRONG_PACKAGES=$((WRONG_PACKAGES + 1))
        fi
    fi
done

if [ $WRONG_PACKAGES -eq 0 ]; then
    echo "✅ All package declarations are correct"
else
    echo "❌ Found $WRONG_PACKAGES files with wrong package declarations"
    exit 1
fi
echo ""

# Test 4: Verify correct imports
echo "Test 4: Verifying imports use store project entities..."
CORRECT_IMPORTS=0

# Check for DeliveryStatus import
if grep -q "import com.restaurant.store.entity.DeliveryStatus" src/main/java/com/restaurant/store/dto/admin/DeliveryDTO.java; then
    echo "✅ DeliveryDTO uses correct DeliveryStatus import"
    CORRECT_IMPORTS=$((CORRECT_IMPORTS + 1))
else
    echo "❌ DeliveryDTO missing or wrong DeliveryStatus import"
fi

# Check for OrderStatus and OrderType imports
if grep -q "import com.restaurant.store.entity.OrderStatus" src/main/java/com/restaurant/store/dto/admin/OrderDTO.java && \
   grep -q "import com.restaurant.store.entity.OrderType" src/main/java/com/restaurant/store/dto/admin/OrderDTO.java; then
    echo "✅ OrderDTO uses correct OrderStatus and OrderType imports"
    CORRECT_IMPORTS=$((CORRECT_IMPORTS + 1))
else
    echo "❌ OrderDTO missing or wrong imports"
fi

# Check for Role import
if grep -q "import com.restaurant.store.entity.Role" src/main/java/com/restaurant/store/dto/admin/UserDTO.java; then
    echo "✅ UserDTO uses correct Role import"
    CORRECT_IMPORTS=$((CORRECT_IMPORTS + 1))
else
    echo "❌ UserDTO missing or wrong Role import"
fi

if [ $CORRECT_IMPORTS -eq 3 ]; then
    echo "✅ All main DTOs have correct imports"
else
    echo "❌ Some DTOs have incorrect imports"
    exit 1
fi
echo ""

# Test 5: Verify Role enum exists
echo "Test 5: Verifying Role enum..."
if [ -f "src/main/java/com/restaurant/store/entity/Role.java" ]; then
    echo "✅ Role enum exists"
    echo "   Enum values:"
    grep -E "^\s*(ADMIN|MANAGER|CHEF|DRIVER)" src/main/java/com/restaurant/store/entity/Role.java | sed 's/^/   - /'
else
    echo "❌ Role enum not found"
    exit 1
fi
echo ""

# Test 6: Verify DTO structure
echo "Test 6: Verifying DTO file structure..."
TOTAL_DTOS=$(find src/main/java/com/restaurant/store/dto/admin -name "*.java" | wc -l)
echo "   Total DTO files found: $TOTAL_DTOS"

MAIN_DTOS=$(find src/main/java/com/restaurant/store/dto/admin -maxdepth 1 -name "*.java" | wc -l)
REQUEST_DTOS=$(find src/main/java/com/restaurant/store/dto/admin/request -name "*.java" 2>/dev/null | wc -l)
RESPONSE_DTOS=$(find src/main/java/com/restaurant/store/dto/admin/response -name "*.java" 2>/dev/null | wc -l)
WEBSOCKET_DTOS=$(find src/main/java/com/restaurant/store/dto/admin/websocket -name "*.java" 2>/dev/null | wc -l)

echo "   Main DTOs: $MAIN_DTOS"
echo "   Request DTOs: $REQUEST_DTOS"
echo "   Response DTOs: $RESPONSE_DTOS"
echo "   WebSocket DTOs: $WEBSOCKET_DTOS"

if [ $MAIN_DTOS -ge 6 ] && [ $REQUEST_DTOS -ge 10 ] && [ $RESPONSE_DTOS -ge 4 ] && [ $WEBSOCKET_DTOS -ge 1 ]; then
    echo "✅ DTO structure is correct"
else
    echo "❌ DTO structure incomplete"
    exit 1
fi
echo ""

# Test 7: Check TypeScript generation
echo "Test 7: Checking TypeScript endpoint generation..."
if [ -f "app/src/endpoints.d.ts" ]; then
    echo "✅ TypeScript endpoints generated successfully"
else
    echo "⚠️  TypeScript endpoints file not found (may not be critical)"
fi
echo ""

echo "=================================="
echo "✅ All Tests Passed!"
echo "=================================="
echo ""
echo "Summary:"
echo "- All admin DTOs compile without errors"
echo "- No references to external admin project"
echo "- All package declarations are correct"
echo "- All imports use store project entities"
echo "- Role enum created and configured"
echo "- DTO structure is properly organized"
echo ""
