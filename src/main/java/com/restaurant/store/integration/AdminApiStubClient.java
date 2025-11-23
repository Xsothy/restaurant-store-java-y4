package com.restaurant.store.integration;

import com.restaurant.store.dto.admin.DeliveryDTO;
import com.restaurant.store.dto.admin.OrderDTO;
import com.restaurant.store.dto.admin.UserDTO;
import com.restaurant.store.entity.Category;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderItem;
import com.restaurant.store.entity.OrderStatus;
import com.restaurant.store.entity.OrderType;
import com.restaurant.store.entity.Product;
import com.restaurant.store.entity.DeliveryStatus;
import com.restaurant.store.integration.dto.AdminCategoryDto;
import com.restaurant.store.integration.dto.AdminProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@ConditionalOnProperty(name = "admin.api.stub.enabled", havingValue = "true")
public class AdminApiStubClient implements AdminIntegrationService {

    @Override
    public List<AdminCategoryDto> fetchCategories() {
        log.info("Using stubbed Admin API categories");

        LocalDateTime now = LocalDateTime.now();

        AdminCategoryDto mains = new AdminCategoryDto();
        mains.setId(1001L);
        mains.setName("Mains");
        mains.setDescription("Signature dishes and chef specials");
        mains.setCreatedAt(now.minusDays(5));
        mains.setUpdatedAt(now.minusDays(1));

        AdminCategoryDto drinks = new AdminCategoryDto();
        drinks.setId(1002L);
        drinks.setName("Beverages");
        drinks.setDescription("Fresh juices, coffees, and teas");
        drinks.setCreatedAt(now.minusDays(5));
        drinks.setUpdatedAt(now.minusHours(12));

        return Arrays.asList(mains, drinks);
    }

    @Override
    public Optional<AdminCategoryDto> fetchCategoryById(Long categoryId) {
        if (categoryId == null) {
            return Optional.empty();
        }
        return fetchCategories().stream()
                .filter(category -> categoryId.equals(category.getId()))
                .findFirst();
    }

    @Override
    public List<AdminProductDto> fetchProducts() {
        log.info("Using stubbed Admin API products");

        LocalDateTime now = LocalDateTime.now();

        AdminProductDto amok = new AdminProductDto();
        amok.setId(2001L);
        amok.setName("Fish Amok");
        amok.setDescription("Traditional Cambodian steamed fish curry");
        amok.setPrice(new BigDecimal("18000"));
        amok.setImageUrl("https://images.example.com/menu/fish-amok.jpg");
        amok.setIsAvailable(true);
        amok.setCategoryId(1001L);
        amok.setCreatedAt(now.minusDays(5));
        amok.setUpdatedAt(now.minusHours(6));

        AdminProductDto lokLak = new AdminProductDto();
        lokLak.setId(2002L);
        lokLak.setName("Beef Lok Lak");
        lokLak.setDescription("Marinated beef with Kampot pepper lime dip");
        lokLak.setPrice(new BigDecimal("21000"));
        lokLak.setImageUrl("https://images.example.com/menu/beef-lok-lak.jpg");
        lokLak.setIsAvailable(true);
        lokLak.setCategoryId(1001L);
        lokLak.setCreatedAt(now.minusDays(4));
        lokLak.setUpdatedAt(now.minusHours(4));

        AdminProductDto icedCoffee = new AdminProductDto();
        icedCoffee.setId(2003L);
        icedCoffee.setName("Iced Khmer Coffee");
        icedCoffee.setDescription("Robust coffee with sweetened condensed milk");
        icedCoffee.setPrice(new BigDecimal("6000"));
        icedCoffee.setImageUrl("https://images.example.com/menu/iced-khmer-coffee.jpg");
        icedCoffee.setIsAvailable(true);
        icedCoffee.setCategoryId(1002L);
        icedCoffee.setCreatedAt(now.minusDays(4));
        icedCoffee.setUpdatedAt(now.minusHours(2));

        return Arrays.asList(amok, lokLak, icedCoffee);
    }

    @Override
    public Optional<Long> pushCategory(Category category) {
        if (category == null) {
            return Optional.empty();
        }
        long externalId = category.getExternalId() != null ? category.getExternalId() : 9000L + category.getId();
        log.info("Stubbed Admin API synced category {} -> external {}", category.getId(), externalId);
        return Optional.of(externalId);
    }

    @Override
    public Optional<Long> pushProduct(Product product) {
        if (product == null) {
            return Optional.empty();
        }
        long externalId = product.getExternalId() != null ? product.getExternalId() : 12000L + product.getId();
        log.info("Stubbed Admin API synced product {} -> external {}", product.getId(), externalId);
        return Optional.of(externalId);
    }

    @Override
    public Optional<Long> syncOrderToAdmin(Order order, List<OrderItem> orderItems) {
        if (order == null) {
            return Optional.empty();
        }
        long externalId = order.getExternalId() != null ? order.getExternalId() : 15000L + order.getId();
        log.info("Stubbed Admin API received order {} for sync -> external {}", order.getId(), externalId);
        return Optional.of(externalId);
    }

    @Override
    public void updateOrderStatus(Order order) {
        if (order == null) {
            return;
        }
        log.info("Stubbed Admin API received status {} for order {}", order.getStatus(), order.getExternalId());
    }

    @Override
    public List<OrderDTO> fetchKitchenOrders() {
        log.info("Stubbed Admin API polling kitchen orders");
        OrderDTO kitchenOrder = OrderDTO.builder()
                .id(31001L)
                .customerName("Sophea Kitchen")
                .customerPhone("+855 12 345 678")
                .customerAddress("Phnom Penh")
                .customerDetails("Sophea | +85512345678")
                .status(OrderStatus.PREPARING)
                .totalPrice(new BigDecimal("25000"))
                .orderType(OrderType.DINE_IN)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .orderItems(Collections.emptyList())
                .build();
        return List.of(kitchenOrder);
    }

    @Override
    public List<OrderDTO> fetchDeliveryOrders() {
        log.info("Stubbed Admin API polling delivery orders");
        DeliveryDTO delivery = DeliveryDTO.builder()
                .id(41001L)
                .orderId(32002L)
                .status(DeliveryStatus.ON_THE_WAY)
                .driver(UserDTO.builder()
                        .id(51001L)
                        .username("driver.chan")
                        .fullName("Chan Dara")
                        .build())
                .deliveryAddress("123 Riverside Blvd")
                .dispatchedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        OrderDTO deliveryOrder = OrderDTO.builder()
                .id(32002L)
                .customerName("Vuthy Delivery")
                .customerPhone("+855 98 765 432")
                .customerAddress("123 Riverside Blvd")
                .customerDetails("Vuthy | +85598765432")
                .status(OrderStatus.OUT_FOR_DELIVERY)
                .totalPrice(new BigDecimal("38000"))
                .orderType(OrderType.DELIVERY)
                .createdAt(LocalDateTime.now().minusMinutes(15))
                .orderItems(Collections.emptyList())
                .delivery(delivery)
                .build();

        return List.of(deliveryOrder);
    }
}
