package com.restaurant.store.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthWebController {
    
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        return "login";
    }
    
    @GetMapping("/register")
    public String register() {
        return "register";
    }
    
    @PostMapping("/login")
    public String processLogin(
            @RequestParam String email,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {
        
        // TODO: Implement session-based login logic
        // This would authenticate the user and create a session
        redirectAttributes.addFlashAttribute("message", "Login successful");
        return "redirect:/menu";
    }
    
    @PostMapping("/register")
    public String processRegister(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String address,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {
        
        // TODO: Implement session-based registration logic
        // This would register the user and create a session
        redirectAttributes.addFlashAttribute("message", "Registration successful");
        return "redirect:/menu";
    }
    
    @PostMapping("/logout")
    public String logout(RedirectAttributes redirectAttributes) {
        // TODO: Implement session-based logout logic
        // This would invalidate the session
        redirectAttributes.addFlashAttribute("message", "Logout successful");
        return "redirect:/menu";
    }
}