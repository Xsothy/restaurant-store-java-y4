package com.restaurant.store.controller;

import com.restaurant.store.dto.request.AddToCartRequest;
import com.restaurant.store.dto.request.UpdateCartItemRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.CartResponse;
import com.restaurant.store.dto.response.ErrorResponse;
import com.restaurant.store.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CartController {
    
    private final CartService cartService;
    
    @Operation(
            summary = "Get cart",
            description = "Retrieves the current user's shopping cart with all items"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cart retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader("Authorization") String authToken) {
        CartResponse cart = cartService.getCart(authToken);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
    }
    
    @Operation(
            summary = "Add item to cart",
            description = "Adds a product to the shopping cart with specified quantity"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Item added to cart successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @RequestHeader("Authorization") String authToken) {
        CartResponse cart = cartService.addToCart(request, authToken);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully", cart));
    }
    
    @Operation(
            summary = "Update cart item",
            description = "Updates the quantity of an item in the cart"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cart item updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Cart item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @Parameter(description = "Cart Item ID") @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @RequestHeader("Authorization") String authToken) {
        CartResponse cart = cartService.updateCartItem(cartItemId, request, authToken);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated successfully", cart));
    }
    
    @Operation(
            summary = "Remove item from cart",
            description = "Removes a specific item from the shopping cart"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Item removed from cart successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Cart item not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @Parameter(description = "Cart Item ID") @PathVariable Long cartItemId,
            @RequestHeader("Authorization") String authToken) {
        CartResponse cart = cartService.removeFromCart(cartItemId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully", cart));
    }
    
    @Operation(
            summary = "Clear cart",
            description = "Removes all items from the shopping cart"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cart cleared successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader("Authorization") String authToken) {
        cartService.clearCart(authToken);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", null));
    }
}
