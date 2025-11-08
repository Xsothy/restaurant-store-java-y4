package com.restaurant.store.controller.web;

import com.restaurant.store.dto.request.CustomerRegisterRequest;
import com.restaurant.store.dto.request.LoginRequest;
import com.restaurant.store.dto.response.AuthResponse;
import com.restaurant.store.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthWebController {
    
    @Autowired
    private AuthService authService;
    
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }
    
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", new CustomerRegisterRequest());
        return "register";
    }
    
    @PostMapping("/login")
    public String processLogin(
            @Valid @ModelAttribute LoginRequest loginRequest,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Authenticate user using AuthService
            AuthResponse authResponse = authService.login(loginRequest);
            
            // Create session for web user
            HttpSession session = request.getSession(true);
            session.setAttribute("customer", authResponse.getCustomer());
            session.setAttribute("token", authResponse.getToken());
            
            redirectAttributes.addFlashAttribute("message", "Login successful");
            return "redirect:/menu";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password");
            return "redirect:/auth/login?error=true";
        }
    }
    
    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute CustomerRegisterRequest registerRequest,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Register user using AuthService
            AuthResponse authResponse = authService.register(registerRequest);
            
            // Create session for web user
            HttpSession session = request.getSession(true);
            session.setAttribute("customer", authResponse.getCustomer());
            session.setAttribute("token", authResponse.getToken());
            
            redirectAttributes.addFlashAttribute("message", "Registration successful");
            return "redirect:/menu";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/auth/register";
        }
    }
    
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("message", "Logout successful");
        return "redirect:/menu";
    }
}