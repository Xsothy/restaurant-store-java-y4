package com.restaurant.store.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for payment result pages (success/cancel).
 */
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebController {

    /**
     * Display payment success page.
     * 
     * @param sessionId Stripe session ID (optional)
     */
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam(required = false) String session_id, Model model) {
        model.addAttribute("sessionId", session_id);
        return "payment-success";
    }

    /**
     * Display payment cancel page.
     * 
     * @param sessionId Stripe session ID (optional)
     */
    @GetMapping("/cancel")
    public String paymentCancel(@RequestParam(required = false) String session_id, Model model) {
        model.addAttribute("sessionId", session_id);
        return "payment-cancel";
    }
}
