package com.restaurant.store.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }
}