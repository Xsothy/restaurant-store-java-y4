package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.entity.OrderStatus;
import com.restaurant.store.entity.OrderType;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.exception.UnauthorizedException;
import com.restaurant.store.security.AuthHelper;
import com.restaurant.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for order-related web pages.
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderWebController {

    private final AuthHelper authHelper;
    private final OrderService orderService;

    private static final List<OrderStatus> DELIVERY_TRACKING_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY_FOR_DELIVERY,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.COMPLETED
    );

    private static final List<OrderStatus> PICKUP_TRACKING_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.COMPLETED
    );

    private static final Map<OrderStatus, String> TRACKING_STATUS_LABELS;

    static {
        EnumMap<OrderStatus, String> labels = new EnumMap<>(OrderStatus.class);
        labels.put(OrderStatus.PENDING, "Pending Confirmation");
        labels.put(OrderStatus.CONFIRMED, "Confirmed");
        labels.put(OrderStatus.PREPARING, "Preparing Order");
        labels.put(OrderStatus.READY_FOR_PICKUP, "Ready for Pickup");
        labels.put(OrderStatus.READY_FOR_DELIVERY, "Ready for Delivery");
        labels.put(OrderStatus.OUT_FOR_DELIVERY, "Out for Delivery");
        labels.put(OrderStatus.COMPLETED, "Completed");
        TRACKING_STATUS_LABELS = Collections.unmodifiableMap(labels);
    }
    
    /**
     * Display orders list page.
     */
    @GetMapping
    public String orders(Model model, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            List<OrderResponse> orders = orderService.getOrdersForCustomer(customer.getId());
            model.addAttribute("orders", orders);

            return "orders";
        } catch (UnauthorizedException ex) {
            log.debug("Unauthorized orders access: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to view your orders");
            return "redirect:/login";
        }
    }
    
    /**
     * Display order details page.
     */
    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            OrderResponse order = orderService.getOrderForCustomer(orderId, customer.getId());

            if (!order.getCustomerId().equals(customer.getId())) {
                log.warn("Customer {} attempted to access order {} belonging to another customer",
                        customer.getId(), orderId);
                redirectAttributes.addFlashAttribute("errorMessage", "You cannot view this order");
                return "redirect:/orders";
            }

            model.addAttribute("order", order);
            applyTrackingAttributes(model, order);
            return "order-details";
        } catch (UnauthorizedException ex) {
            log.debug("Unauthorized order details access: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to view your orders");
            return "redirect:/login";
        } catch (BadRequestException | ResourceNotFoundException ex) {
            log.warn("Unable to load order {} for details view: {}", orderId, ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("order", null);
            applyTrackingAttributes(model, null);
            return "order-details";
        }
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            orderService.cancelOrderForCustomer(orderId, customer.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Order cancelled successfully");
            return "redirect:/orders/" + orderId;
        } catch (UnauthorizedException ex) {
            log.debug("Unauthorized order cancellation attempt: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to manage your orders");
            return "redirect:/login";
        } catch (BadRequestException | ResourceNotFoundException ex) {
            log.warn("Failed to cancel order {}: {}", orderId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders/" + orderId;
        }
    }

    private void applyTrackingAttributes(Model model, OrderResponse order) {
        List<OrderStatus> trackingStatuses = determineTrackingStatuses(order);
        model.addAttribute("trackingStatuses", trackingStatuses);
        model.addAttribute("statusLabels", TRACKING_STATUS_LABELS);

        int stepCount = trackingStatuses.size();
        boolean hasOrder = order != null;
        boolean isCancelled = hasOrder && order.getStatus() == OrderStatus.CANCELLED;
        int currentIndex = hasOrder ? trackingStatuses.indexOf(order.getStatus()) : -1;
        boolean hasValidStatus = currentIndex >= 0;
        int completedSteps = (!isCancelled && hasValidStatus) ? currentIndex + 1 : 0;
        double progressPercent = (!isCancelled && hasValidStatus && stepCount > 0)
                ? (completedSteps * 100.0) / stepCount
                : 0;
        OrderStatus nextStep = (!isCancelled && hasValidStatus && currentIndex < stepCount - 1)
                ? trackingStatuses.get(currentIndex + 1)
                : null;
        List<OrderStatus> remainingSteps = (!isCancelled && hasValidStatus && currentIndex < stepCount - 1)
                ? trackingStatuses.subList(currentIndex + 1, stepCount)
                : Collections.emptyList();

        model.addAttribute("trackingStepCount", stepCount);
        model.addAttribute("trackingIsCancelled", isCancelled);
        model.addAttribute("trackingHasValidStatus", hasValidStatus);
        model.addAttribute("trackingCurrentIndex", currentIndex);
        model.addAttribute("trackingCompletedSteps", completedSteps);
        model.addAttribute("trackingProgressPercent", progressPercent);
        model.addAttribute("trackingNextStep", nextStep);
        model.addAttribute("trackingRemainingSteps", remainingSteps);
    }

    private List<OrderStatus> determineTrackingStatuses(OrderResponse order) {
        if (order == null || order.getOrderType() == null) {
            return DELIVERY_TRACKING_STATUSES;
        }

        return order.getOrderType() == OrderType.PICKUP
                ? PICKUP_TRACKING_STATUSES
                : DELIVERY_TRACKING_STATUSES;
    }
}