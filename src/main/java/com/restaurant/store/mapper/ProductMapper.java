package com.restaurant.store.mapper;

import com.restaurant.store.dto.response.ProductResponse;
import com.restaurant.store.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    
    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .isAvailable(product.getIsAvailable())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }
}
