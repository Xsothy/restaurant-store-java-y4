package com.restaurant.store.repository;

import com.restaurant.store.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByCategoryId(Long categoryId);
    
    List<Product> findByIsAvailableTrue();

    List<Product> findByIsAvailableTrueAndCategoryId(Long categoryId);
    
    Optional<Product> findByExternalId(Long externalId);
}