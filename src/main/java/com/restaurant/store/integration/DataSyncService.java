package com.restaurant.store.integration;

import com.restaurant.store.entity.Category;
import com.restaurant.store.entity.Product;
import com.restaurant.store.integration.dto.AdminCategoryDto;
import com.restaurant.store.integration.dto.AdminProductDto;
import com.restaurant.store.repository.CategoryRepository;
import com.restaurant.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSyncService {

    private final AdminIntegrationService adminIntegrationService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Value("${admin.api.sync.enabled}")
    private boolean syncEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (syncEnabled) {
            log.info("Application ready - initiating startup data sync");
            syncAllData();
        }
    }

    @Scheduled(fixedDelayString = "${admin.api.sync.interval}")
    public void scheduledSync() {
        if (syncEnabled) {
            log.info("Running scheduled data sync");
            syncAllData();
        }
    }

    @Transactional
    public void syncAllData() {
        log.info("Starting data sync from Admin API");
        try {
            syncCategories();
            syncProducts();
            log.info("Data sync completed successfully");
        } catch (Exception e) {
            log.error("Error during data sync", e);
        }
    }

    @Transactional
    public void syncCategories() {
        log.info("Syncing categories from Admin API");
        List<AdminCategoryDto> adminCategories = adminIntegrationService.fetchCategories();

        if (adminCategories.isEmpty()) {
            log.warn("No categories to sync");
            return;
        }

        for (AdminCategoryDto adminCategory : adminCategories) {
            Optional<Category> existingCategory = categoryRepository.findByExternalId(adminCategory.getId());

            Category category;
            if (existingCategory.isPresent()) {
                category = existingCategory.get();
                log.debug("Updating existing category: {}", category.getName());
            } else {
                category = new Category();
                category.setExternalId(adminCategory.getId());
                log.debug("Creating new category: {}", adminCategory.getName());
            }

            category.setName(adminCategory.getName());
            category.setDescription(adminCategory.getDescription());
            category.setSyncedAt(LocalDateTime.now());

            categoryRepository.save(category);
        }

        log.info("Synced {} categories", adminCategories.size());
    }

    @Transactional
    public void syncProducts() {
        log.info("Syncing products from Admin API");
        List<AdminProductDto> adminProducts = adminIntegrationService.fetchProducts();

        if (adminProducts.isEmpty()) {
            log.warn("No products to sync");
            return;
        }

        Map<Long, Category> categoryCache = new HashMap<>();

        for (AdminProductDto adminProduct : adminProducts) {
            Optional<Product> existingProduct = productRepository.findByExternalId(adminProduct.getId());

            Category category = categoryCache.computeIfAbsent(
                    adminProduct.getCategoryId(),
                    id -> categoryRepository.findByExternalId(id)
                            .orElseThrow(() -> new RuntimeException("Category not found for external ID: " + id))
            );

            Product product;
            if (existingProduct.isPresent()) {
                product = existingProduct.get();
                log.debug("Updating existing product: {}", product.getName());
            } else {
                product = new Product();
                product.setExternalId(adminProduct.getId());
                log.debug("Creating new product: {}", adminProduct.getName());
            }

            product.setName(adminProduct.getName());
            product.setDescription(adminProduct.getDescription());
            product.setPrice(adminProduct.getPrice());
            product.setImageUrl(adminProduct.getImageUrl());
            product.setIsAvailable(adminProduct.getIsAvailable());
            product.setCategory(category);
            product.setSyncedAt(LocalDateTime.now());

            productRepository.save(product);
        }

        log.info("Synced {} products", adminProducts.size());
    }
}
