package com.restaurant.store.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Main web controller for miscellaneous routes.
 * Most routes have been moved to dedicated controllers:
 * - Auth: AuthWebController, HomeWebController
 * - Cart: CartWebController
 * - Checkout: CheckoutWebController
 * - Orders: OrderWebController
 * - Profile: ProfileWebController
 * - Payment: PaymentWebController
 * - Products/Menu: MenuController
 */
@Controller
public class WebController {

    /**
     * Redirect root to menu.
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/menu";
    }
}
