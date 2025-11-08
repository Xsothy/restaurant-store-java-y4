package com.restaurant.store.controller;

import com.restaurant.store.dto.response.CategoryResponse;
import com.restaurant.store.dto.response.ProductResponse;
import com.restaurant.store.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {
    
    private final ProductService productService;
    
    @Value("${stripe.api.key}")
    private String stripePublicKey;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping({"/", "/menu"})
    public String menu(Model model) {
        List<CategoryResponse> categories = productService.getAllCategories();
        List<ProductResponse> products = productService.getAllProducts(null, true);
        
        model.addAttribute("categories", categories);
        model.addAttribute("products", products);
        return "menu";
    }

    @GetMapping("/products/{productId}")
    public String productDetails(@PathVariable Long productId, Model model) {
        ProductResponse product = productService.getProductById(productId);
        model.addAttribute("product", product);
        return "product-details";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }
    
    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("stripePublicKey", stripePublicKey);
        return "checkout";
    }

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }
    
    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam(required = false) String session_id, Model model) {
        model.addAttribute("sessionId", session_id);
        return "payment-success";
    }
    
    @GetMapping("/payment/cancel")
    public String paymentCancel(@RequestParam(required = false) String session_id, Model model) {
        model.addAttribute("sessionId", session_id);
        return "payment-cancel";
    }
}
