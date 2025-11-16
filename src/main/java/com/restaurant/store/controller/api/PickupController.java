package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.PickupResponse;
import com.restaurant.store.service.PickupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pickups")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Pickups", description = "Pickup order tracking endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PickupController {

    private final PickupService pickupService;

    @Operation(
            summary = "Get pickup details",
            description = "Retrieves pickup window and instructions for a pickup order"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Pickup details retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Order is not a pickup order"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Pickup or order not found"
            )
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PickupResponse>> getPickupDetails(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {

        PickupResponse response = pickupService.getPickupDetails(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Pickup details retrieved successfully", response));
    }
}
