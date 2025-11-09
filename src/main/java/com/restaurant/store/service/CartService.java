package com.restaurant.store.service;

import com.restaurant.store.dto.request.AddToCartRequest;
import com.restaurant.store.dto.request.UpdateCartItemRequest;
import com.restaurant.store.dto.response.CartItemResponse;
import com.restaurant.store.dto.response.CartResponse;
import com.restaurant.store.entity.*;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.repository.CartItemRepository;
import com.restaurant.store.repository.CartRepository;
import com.restaurant.store.repository.CustomerRepository;
import com.restaurant.store.repository.ProductRepository;
import com.restaurant.store.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;
    
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("6000.00");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.10");
    
    @Transactional
    public CartResponse addToCart(AddToCartRequest request, String token) {
        Customer customer = getCustomerFromToken(token);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        if (!product.getIsAvailable()) {
            throw new BadRequestException("Product is not available");
        }
        
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> createNewCart(customer));
        
        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);
        
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(product.getPrice());
            cartItemRepository.save(cartItem);
        }
        
        return getCartResponse(cart.getId());
    }
    
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request, String token) {
        Customer customer = getCustomerFromToken(token);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (!cartItem.getCart().getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Cart item does not belong to current customer");
        }
        
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);
        
        return getCartResponse(cartItem.getCart().getId());
    }
    
    @Transactional
    public CartResponse removeFromCart(Long cartItemId, String token) {
        Customer customer = getCustomerFromToken(token);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (!cartItem.getCart().getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Cart item does not belong to current customer");
        }
        
        Long cartId = cartItem.getCart().getId();
        cartItemRepository.delete(cartItem);
        
        return getCartResponse(cartId);
    }
    
    public CartResponse getCart(String token) {
        Customer customer = getCustomerFromToken(token);
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> createNewCart(customer));
        
        return getCartResponse(cart.getId());
    }
    
    /**
     * Get cart by customer ID (for internal use, e.g., web controllers).
     */
    public CartResponse getCartByCustomerId(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElseGet(() -> createNewCart(customer));
        
        return getCartResponse(cart.getId());
    }
    
    @Transactional
    public void clearCart(String token) {
        Customer customer = getCustomerFromToken(token);
        Cart cart = cartRepository.findByCustomerId(customer.getId())
                .orElse(null);
        
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
        }
    }
    
    @Transactional
    public void clearCartForCustomer(Long customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElse(null);
        
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
        }
    }
    
    private Customer getCustomerFromToken(String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
    
    private Cart createNewCart(Customer customer) {
        Cart cart = new Cart();
        cart.setCustomer(customer);
        return cartRepository.save(cart);
    }
    
    private CartResponse getCartResponse(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        
        List<CartItem> items = cartItemRepository.findByCartId(cartId);
        
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());
        
        BigDecimal subtotal = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        int itemCount = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        BigDecimal vat = subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deliveryFee = itemCount > 0 ? DELIVERY_FEE : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(vat).add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .subtotal(subtotal)
                .vat(vat)
                .deliveryFee(deliveryFee)
                .total(total)
                .itemCount(itemCount)
                .build();
    }
    
    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        BigDecimal subtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
