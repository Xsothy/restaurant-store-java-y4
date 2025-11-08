package com.restaurant.store.integration;

import com.restaurant.store.integration.dto.AdminApiResponse;
import com.restaurant.store.integration.dto.AdminCategoryDto;
import com.restaurant.store.integration.dto.AdminProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminApiClient {

    private final WebClient adminWebClient;

    public List<AdminCategoryDto> fetchCategories() {
        try {
            log.info("Fetching categories from Admin API");
            AdminApiResponse<List<AdminCategoryDto>> response = adminWebClient.get()
                    .uri("/categories")
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

    public List<AdminProductDto> fetchProducts() {
        try {
            log.info("Fetching products from Admin API");
            AdminApiResponse<List<AdminProductDto>> response = adminWebClient.get()
                    .uri("/products")
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

    public void syncOrderToAdmin(Long orderId) {
        try {
            log.info("Syncing order {} to Admin API", orderId);
            // This would be implemented based on Admin API's order creation endpoint
            // For now, we'll just log it
            log.info("Order sync placeholder - to be implemented");
        } catch (Exception e) {
            log.error("Error syncing order to Admin API", e);
        }
    }
}
