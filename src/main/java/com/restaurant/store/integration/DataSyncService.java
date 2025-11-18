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
import org.springframework.context.ApplicationContext;
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
    private final ApplicationContext applicationContext;

    @Value("${admin.api.sync.enabled}")
    private boolean syncEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (syncEnabled) {
            log.info("Application ready - initiating startup data sync");
            // Call through the Spring proxy so @Transactional on syncAllData is applied
            applicationContext.getBean(DataSyncService.class).syncAllData();
        }
    }

    @Scheduled(fixedDelayString = "${admin.api.sync.interval}")
    public void scheduledSync() {
        if (syncEnabled) {
            log.info("Running scheduled data sync");
            // Call through the Spring proxy so @Transactional on syncAllData is applied
            applicationContext.getBean(DataSyncService.class).syncAllData();
        }
    }

    @Transactional
    public void syncAllData() {
        log.info("Starting data sync from Admin API");
        try {
//            syncCategoriesFromAdmin();
//            syncProductsFromAdmin();
            pushLocalCategoriesToAdmin();
            pushLocalProductsToAdmin();
            log.info("Data sync completed successfully");
        } catch (Exception e) {
            log.error("Error during data sync", e);
        }
    }

    @Transactional
    public void syncCategories() {
        syncCategoriesFromAdmin();
        pushLocalCategoriesToAdmin();
    }

    @Transactional
    public void syncProducts() {
        syncProductsFromAdmin();
        pushLocalProductsToAdmin();
    }

    private void syncCategoriesFromAdmin() {
        log.info("Syncing categories from Admin API");
        List<AdminCategoryDto> adminCategories = adminIntegrationService.fetchCategories();

        if (adminCategories.isEmpty()) {
            log.warn("No categories to sync from Admin API");
            return;
        }

        for (AdminCategoryDto adminCategory : adminCategories) {
            Optional<Category> existingCategory = categoryRepository.findByExternalId(adminCategory.getId());

            Category category = existingCategory.orElseGet(Category::new);
            if (category.getExternalId() == null) {
                category.setExternalId(adminCategory.getId());
            }

            category.setName(adminCategory.getName());
            category.setDescription(adminCategory.getDescription());
            category.setSyncedAt(LocalDateTime.now());

            categoryRepository.save(category);
        }

        log.info("Pulled {} categories from Admin API", adminCategories.size());
    }

    private void syncProductsFromAdmin() {
        log.info("Syncing products from Admin API");
        List<AdminProductDto> adminProducts = adminIntegrationService.fetchProducts();

        if (adminProducts.isEmpty()) {
            log.warn("No products to sync from Admin API");
            return;
        }

        Map<Long, Category> categoryCache = new HashMap<>();

        for (AdminProductDto adminProduct : adminProducts) {
            Optional<Product> existingProduct = productRepository.findByExternalId(adminProduct.getId());

            Category category = categoryCache.computeIfAbsent(
                    adminProduct.getCategoryId(),
                    this::resolveCategory
            );

            if (category == null) {
                log.warn("Skipping product {} because category {} could not be resolved", adminProduct.getId(), adminProduct.getCategoryId());
                continue;
            }

            Product product = existingProduct.orElseGet(Product::new);
            if (product.getExternalId() == null) {
                product.setExternalId(adminProduct.getId());
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

        log.info("Pulled {} products from Admin API", adminProducts.size());
    }

    private void pushLocalCategoriesToAdmin() {
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            return;
        }

        for (Category category : categories) {
            boolean needsSync = category.getExternalId() == null ||
                    (category.getUpdatedAt() != null && (category.getSyncedAt() == null || category.getUpdatedAt().isAfter(category.getSyncedAt())));

            if (!needsSync) {
                continue;
            }

            try {
                adminIntegrationService.pushCategory(category).ifPresent(externalId -> {
                    category.setExternalId(externalId);
                    category.setSyncedAt(LocalDateTime.now());
                    categoryRepository.save(category);
                });
            } catch (Exception e) {
                log.error("Failed to push category {} to Admin API", category.getId(), e);
            }
        }
    }

    private void pushLocalProductsToAdmin() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return;
        }

        for (Product product : products) {
            boolean needsSync = product.getExternalId() == null ||
                    (product.getUpdatedAt() != null && (product.getSyncedAt() == null || product.getUpdatedAt().isAfter(product.getSyncedAt())));

            if (!needsSync) {
                continue;
            }

            Category category = product.getCategory();
            if (category != null) {
                Long categoryId = category.getId();
                Category managedCategory = categoryId != null
                        ? categoryRepository.findById(categoryId).orElse(null)
                        : null;

                if (managedCategory != null && managedCategory.getExternalId() == null) {
                    adminIntegrationService.pushCategory(managedCategory).ifPresent(externalId -> {
                        managedCategory.setExternalId(externalId);
                        managedCategory.setSyncedAt(LocalDateTime.now());
                        categoryRepository.save(managedCategory);
                    });
                }
            }

            try {
                adminIntegrationService.pushProduct(product).ifPresent(externalId -> {
                    product.setExternalId(externalId);
                    product.setSyncedAt(LocalDateTime.now());
                    productRepository.save(product);
                });
            } catch (Exception e) {
                log.error("Failed to push product {} to Admin API", product.getId(), e);
            }
        }
    }

    private Category resolveCategory(Long externalCategoryId) {
        if (externalCategoryId == null) {
            return null;
        }

        return categoryRepository.findByExternalId(externalCategoryId)
                .orElseGet(() -> fetchAndPersistCategory(externalCategoryId));
    }

    private Category fetchAndPersistCategory(Long externalCategoryId) {
        return adminIntegrationService.fetchCategoryById(externalCategoryId)
                .map(adminCategory -> {
                    Category category = new Category();
                    category.setExternalId(adminCategory.getId());
                    category.setName(adminCategory.getName());
                    category.setDescription(adminCategory.getDescription());
                    category.setSyncedAt(LocalDateTime.now());
                    return categoryRepository.save(category);
                })
                .orElse(null);
    }
}
