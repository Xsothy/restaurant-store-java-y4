package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.CategoryResponse;
import com.restaurant.store.dto.response.ProductResponse;
import com.restaurant.store.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping("/categories")
    public List<CategoryResponse> getAllCategories() {
        return productService.getAllCategories();
    }

    @GetMapping("/products")
    public List<ProductResponse> getAllProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean availableOnly) {
        
        return productService.getAllProducts(categoryId, availableOnly);
    }
    
    @GetMapping("/products/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        return productService.getProductById(productId);
    }
    
    @GetMapping("/categories/{categoryId}/products")
    public List<ProductResponse> getProductsByCategory(@PathVariable Long categoryId) {
        return productService.getProductsByCategory(categoryId);
    }
}