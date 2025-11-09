package com.restaurant.store.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for root-level routes (/, /login, /register).
 * Provides convenient access to commonly used pages.
 */
@Controller
public class HomeWebController {

    /**
     * Display login page at root level /login.
     * Redirects to /auth/login.
     */
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        return "login";
    }

    /**
     * Display registration page at root level /register.
     * Redirects to /auth/register.
     */
    @GetMapping("/register")
    public String register() {
        return "register";
    }
}
