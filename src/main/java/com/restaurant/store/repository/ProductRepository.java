package com.restaurant.store.repository;

import com.restaurant.store.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByCategoryId(Long categoryId);
    
    List<Product> findByIsAvailableTrue();
    
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isAvailable = true")
    List<Product> findAvailableProductsByCategory(Long categoryId);
}