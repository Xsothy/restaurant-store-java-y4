package com.restaurant.store.controller;

import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.ErrorResponse;
import com.restaurant.store.dto.response.ProductResponse;
import com.restaurant.store.entity.Category;
import com.restaurant.store.service.ProductService;
import com.restaurant.store.dto.response.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "Products & Categories", description = "Endpoints for browsing menu, categories, and products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @Operation(
            summary = "Get all categories",
            description = "Retrieves all product categories available in the menu"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Categories retrieved successfully"
            )
    })
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        
        List<CategoryResponse> categories = productService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(
            summary = "Get all products",
            description = "Retrieves all products with optional filtering by category and availability"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Products retrieved successfully"
            )
    })
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter to show only available products") @RequestParam(required = false) Boolean availableOnly) {
        
        List<ProductResponse> products = productService.getAllProducts(categoryId, availableOnly);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }
    
    @Operation(
            summary = "Get product by ID",
            description = "Retrieves detailed information about a specific product"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Product retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        
        ProductResponse product = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }
    
    @Operation(
            summary = "Get products by category",
            description = "Retrieves all products belonging to a specific category"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Category products retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Category not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/categories/{categoryId}/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(
            @Parameter(description = "Category ID") @PathVariable Long categoryId) {
        
        List<ProductResponse> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category products retrieved successfully", products));
    }
}