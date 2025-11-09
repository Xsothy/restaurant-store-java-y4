package com.restaurant.store.integration;

import com.restaurant.store.dto.admin.request.LoginRequestDTO;
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
}
