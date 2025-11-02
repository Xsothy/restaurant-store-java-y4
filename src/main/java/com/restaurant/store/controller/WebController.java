package com.restaurant.store.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/menu"})
    public String menu() {
        return "menu";
    }

    @GetMapping("/products/{productId}")
    public String productDetails(@PathVariable Long productId, Model model) {
        model.addAttribute("productId", productId);
        return "product-details";
    }
}
