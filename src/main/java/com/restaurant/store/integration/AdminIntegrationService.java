package com.restaurant.store.integration;

import com.restaurant.store.entity.Category;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderItem;
import com.restaurant.store.entity.Product;
import com.restaurant.store.integration.dto.AdminCategoryDto;
import com.restaurant.store.integration.dto.AdminProductDto;

import java.util.List;
import java.util.Optional;

public interface AdminIntegrationService {

    List<AdminCategoryDto> fetchCategories();

    Optional<AdminCategoryDto> fetchCategoryById(Long categoryId);

    List<AdminProductDto> fetchProducts();

    Optional<Long> pushCategory(Category category);

    Optional<Long> pushProduct(Product product);

    Optional<Long> syncOrderToAdmin(Order order, List<OrderItem> orderItems);

    void updateOrderStatus(Order order);
}
