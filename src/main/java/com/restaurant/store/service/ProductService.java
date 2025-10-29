package com.restaurant.store.service;

import com.restaurant.store.dto.response.ProductResponse;
import com.restaurant.store.entity.Category;
import com.restaurant.store.entity.Product;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.mapper.ProductMapper;
import com.restaurant.store.repository.CategoryRepository;
import com.restaurant.store.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductMapper productMapper;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<ProductResponse> getAllProducts(Long categoryId, Boolean availableOnly) {
        List<Product> products;

        if (categoryId != null && availableOnly != null && availableOnly) {
            products = productRepository.findByIsAvailableTrueAndCategoryId(categoryId);
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else if (availableOnly != null && availableOnly) {
            products = productRepository.findByIsAvailableTrue();
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        // Verify category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        List<Product> products = productRepository.findByCategoryId(categoryId);
        return products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
}

