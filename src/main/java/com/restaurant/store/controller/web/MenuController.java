package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.CategoryResponse;
import com.restaurant.store.dto.response.ProductResponse;
import com.restaurant.store.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping
public class MenuController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping({"/", "/menu"})
    public String menu(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean availableOnly,
            Model model) {
        
        List<CategoryResponse> categories = productService.getAllCategories();
        List<ProductResponse> products = productService.getAllProducts(categoryId, availableOnly);
        
        model.addAttribute("categories", categories);
        model.addAttribute("products", products);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("availableOnly", availableOnly != null ? availableOnly : false);
        
        return "menu";
    }
    
    @GetMapping("/products/{productId}")
    public String productDetails(@PathVariable Long productId, Model model) {
        ProductResponse product = productService.getProductById(productId);
        model.addAttribute("product", product);
        return "product-details";
    }
}