package com.restaurant.store.controller;

import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.ProductResponse;
import com.restaurant.store.entity.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class ProductController {
    
    // TODO: Inject ProductService when implemented
    // private final ProductService productService;
    
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        
        // TODO: Implement get all categories logic
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", null));
    }
    
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean availableOnly) {
        
        // TODO: Implement get all products logic with optional filters
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", null));
    }
    
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable Long productId) {
        
        // TODO: Implement get single product logic
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", null));
    }
    
    @GetMapping("/categories/{categoryId}/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId) {
        
        // TODO: Implement get products by category logic
        return ResponseEntity.ok(ApiResponse.success("Category products retrieved successfully", null));
    }
}