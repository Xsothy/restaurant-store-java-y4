package com.restaurant.store.controller;

import com.restaurant.store.dto.request.AddToCartRequest;
import com.restaurant.store.dto.request.UpdateCartItemRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.CartResponse;
import com.restaurant.store.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader("Authorization") String authToken) {
        CartResponse cart = cartService.getCart(authToken);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
    }
    
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @RequestHeader("Authorization") String authToken) {
        CartResponse cart = cartService.addToCart(request, authToken);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully", cart));
    }
    
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @RequestHeader("Authorization") String authToken) {
        CartResponse cart = cartService.updateCartItem(cartItemId, request, authToken);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated successfully", cart));
    }
    
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long cartItemId,
            @RequestHeader("Authorization") String authToken) {
        CartResponse cart = cartService.removeFromCart(cartItemId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully", cart));
    }
    
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader("Authorization") String authToken) {
        cartService.clearCart(authToken);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", null));
    }
}
