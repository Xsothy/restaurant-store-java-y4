package com.restaurant.store.integration;

import com.restaurant.store.integration.dto.AdminCategoryDto;
import com.restaurant.store.integration.dto.AdminProductDto;

import java.util.List;

public interface AdminIntegrationService {

    List<AdminCategoryDto> fetchCategories();

    List<AdminProductDto> fetchProducts();

    void syncOrderToAdmin(Long orderId);
}
