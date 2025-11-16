package com.restaurant.store.integration;

import com.restaurant.store.dto.admin.CategoryDTO;
import com.restaurant.store.dto.admin.OrderDTO;
import com.restaurant.store.dto.admin.ProductDTO;
import com.restaurant.store.dto.admin.request.CreateOrderItemRequestDTO;
import com.restaurant.store.dto.admin.request.CreateOrderRequestDTO;
import com.restaurant.store.dto.admin.request.LoginRequestDTO;
import com.restaurant.store.dto.admin.request.UpdateOrderStatusRequestDTO;
import com.restaurant.store.entity.Category;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderItem;
import com.restaurant.store.entity.Product;
import com.restaurant.store.integration.dto.AdminApiResponse;
import com.restaurant.store.integration.dto.AdminCategoryDto;
import com.restaurant.store.integration.dto.AdminLoginResponse;
import com.restaurant.store.integration.dto.AdminProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "admin.api.stub.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class AdminApiClient implements AdminIntegrationService {

    private final WebClient adminWebClient;

    @Value("${admin.api.username}")
    private String adminUsername;

    @Value("${admin.api.password}")
    private String adminPassword;

    @Override
    public List<AdminCategoryDto> fetchCategories() {
        try {
            log.info("Fetching categories from Admin API");
            String token = authenticate();
            if (token == null) {
                log.warn("Unable to authenticate with Admin API while fetching categories");
                return Collections.emptyList();
            }

            AdminApiResponse<List<AdminCategoryDto>> response = adminWebClient.get()
                    .uri("/categories")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AdminApiResponse<List<AdminCategoryDto>>>() {})
                    .block();

            if (response != null && response.getSuccess() && response.getData() != null) {
                log.info("Successfully fetched {} categories", response.getData().size());
                return response.getData();
            }

            log.warn("No categories returned from Admin API");
            return Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.error("Error fetching categories from Admin API: {} - {}", e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error fetching categories from Admin API", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<AdminProductDto> fetchProducts() {
        try {
            log.info("Fetching products from Admin API");
            String token = authenticate();
            if (token == null) {
                log.warn("Unable to authenticate with Admin API while fetching products");
                return Collections.emptyList();
            }

            AdminApiResponse<List<AdminProductDto>> response = adminWebClient.get()
                    .uri("/products")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AdminApiResponse<List<AdminProductDto>>>() {})
                    .block();

            if (response != null && response.getSuccess() && response.getData() != null) {
                log.info("Successfully fetched {} products", response.getData().size());
                return response.getData();
            }

            log.warn("No products returned from Admin API");
            return Collections.emptyList();
        } catch (WebClientResponseException e) {
            log.error("Error fetching products from Admin API: {} - {}", e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error fetching products from Admin API", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<Long> pushCategory(Category category) {
        if (category == null) {
            return Optional.empty();
        }

        try {
            String token = authenticate();
            if (token == null) {
                log.warn("Unable to authenticate with Admin API while pushing category {}", category.getId());
                return Optional.empty();
            }

            CategoryDTO payload = CategoryDTO.builder()
                    .name(category.getName())
                    .description(category.getDescription())
                    .build();

            ParameterizedTypeReference<AdminApiResponse<CategoryDTO>> responseType =
                    new ParameterizedTypeReference<>() {};

            AdminApiResponse<CategoryDTO> response = (category.getExternalId() == null)
                    ? adminWebClient.post()
                    .uri("/categories")
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block()
                    : adminWebClient.put()
                    .uri("/categories/{id}", category.getExternalId())
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getData() != null) {
                Long externalId = response.getData().getId();
                log.info("Category {} synced to Admin with external ID {}", category.getId(), externalId);
                return Optional.ofNullable(externalId);
            }
        } catch (WebClientResponseException e) {
            log.error("Error pushing category {} to Admin API: {} - {}", category.getId(), e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error pushing category {} to Admin API", category.getId(), e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Long> pushProduct(Product product) {
        if (product == null) {
            return Optional.empty();
        }

        Category category = product.getCategory();
        if (category == null || category.getExternalId() == null) {
            log.warn("Cannot sync product {} because its category is not synced", product.getId());
            return Optional.empty();
        }

        try {
            String token = authenticate();
            if (token == null) {
                log.warn("Unable to authenticate with Admin API while pushing product {}", product.getId());
                return Optional.empty();
            }

            ProductDTO payload = ProductDTO.builder()
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .available(product.getIsAvailable())
                    .imageUrl(product.getImageUrl())
                    .category(CategoryDTO.builder()
                            .id(category.getExternalId())
                            .name(category.getName())
                            .description(category.getDescription())
                            .build())
                    .build();

            ParameterizedTypeReference<AdminApiResponse<ProductDTO>> responseType =
                    new ParameterizedTypeReference<>() {};

            AdminApiResponse<ProductDTO> response = (product.getExternalId() == null)
                    ? adminWebClient.post()
                    .uri("/products")
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block()
                    : adminWebClient.put()
                    .uri("/products/{id}", product.getExternalId())
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getData() != null) {
                Long externalId = response.getData().getId();
                log.info("Product {} synced to Admin with external ID {}", product.getId(), externalId);
                return Optional.ofNullable(externalId);
            }
        } catch (WebClientResponseException e) {
            log.error("Error pushing product {} to Admin API: {} - {}", product.getId(), e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error pushing product {} to Admin API", product.getId(), e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Long> syncOrderToAdmin(Order order, List<OrderItem> orderItems) {
        if (order == null || orderItems == null || orderItems.isEmpty()) {
            return Optional.empty();
        }

        try {
            String token = authenticate();
            if (token == null) {
                log.warn("Unable to authenticate with Admin API while syncing order {}", order.getId());
                return Optional.empty();
            }

            List<CreateOrderItemRequestDTO> itemsPayload = buildOrderItemsPayload(orderItems);
            if (itemsPayload.isEmpty()) {
                log.warn("Skipping Admin order sync for {} because required product identifiers are missing", order.getId());
                return Optional.empty();
            }

            CreateOrderRequestDTO payload = CreateOrderRequestDTO.builder()
                    .customerName(order.getCustomer().getName())
                    .customerPhone(resolveCustomerPhone(order))
                    .customerAddress(resolveCustomerAddress(order))
                    .notes(order.getSpecialInstructions())
                    .totalAmount(order.getTotalPrice())
                    .orderType(order.getOrderType())
                    .items(itemsPayload)
                    .build();

            AdminApiResponse<OrderDTO> response = adminWebClient.post()
                    .uri("/orders")
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<AdminApiResponse<OrderDTO>>() {})
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getData() != null) {
                Long externalId = response.getData().getId();
                log.info("Order {} synced to Admin with external ID {}", order.getId(), externalId);
                return Optional.ofNullable(externalId);
            }
        } catch (WebClientResponseException e) {
            log.error("Error syncing order {} to Admin API: {} - {}", order.getId(), e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error syncing order {} to Admin API", order.getId(), e);
        }

        return Optional.empty();
    }

    @Override
    public void updateOrderStatus(Order order) {
        if (order == null || order.getExternalId() == null) {
            return;
        }

        try {
            String token = authenticate();
            if (token == null) {
                log.warn("Unable to authenticate with Admin API while updating order status for {}", order.getId());
                return;
            }

            UpdateOrderStatusRequestDTO request = UpdateOrderStatusRequestDTO.builder()
                    .status(order.getStatus())
                    .build();

            adminWebClient.patch()
                    .uri("/orders/{id}/status", order.getExternalId())
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error forwarding status for order {} to Admin API: {} - {}", order.getId(), e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error forwarding status for order {} to Admin API", order.getId(), e);
        }
    }

    private String authenticate() {
        try {
            LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                    .username(adminUsername)
                    .password(adminPassword)
                    .build();

            AdminLoginResponse response = adminWebClient.post()
                    .uri("/auth/login")
                    .bodyValue(loginRequest)
                    .retrieve()
                    .bodyToMono(AdminLoginResponse.class)
                    .block();

            if (response != null && Boolean.TRUE.equals(response.getSuccess()) && response.getToken() != null) {
                return response.getToken();
            }

            if (response != null) {
                log.warn("Admin API authentication failed: {}", response.getMessage());
            } else {
                log.warn("Admin API authentication returned no response");
            }
        } catch (WebClientResponseException e) {
            log.error("Admin API authentication error: {} - {}", e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during Admin API authentication", e);
        }

        return null;
    }

    private List<CreateOrderItemRequestDTO> buildOrderItemsPayload(List<OrderItem> orderItems) {
        boolean hasMissingProduct = orderItems.stream()
                .anyMatch(item -> item.getProduct() == null || item.getProduct().getExternalId() == null);

        if (hasMissingProduct) {
            return Collections.emptyList();
        }

        return orderItems.stream()
                .map(item -> CreateOrderItemRequestDTO.builder()
                        .productId(item.getProduct().getExternalId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveCustomerPhone(Order order) {
        if (order.getPhoneNumber() != null && !order.getPhoneNumber().isBlank()) {
            return order.getPhoneNumber();
        }
        String fallback = order.getCustomer().getPhone();
        return (fallback == null || fallback.isBlank()) ? "N/A" : fallback;
    }

    private String resolveCustomerAddress(Order order) {
        if (order.getDeliveryAddress() != null && !order.getDeliveryAddress().isBlank()) {
            return order.getDeliveryAddress();
        }
        return order.getCustomer().getAddress();
    }
}
