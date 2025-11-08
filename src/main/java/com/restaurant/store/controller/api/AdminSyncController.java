package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.integration.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Slf4j
public class AdminSyncController {

    private final DataSyncService dataSyncService;

    @PostMapping("/all")
    public ResponseEntity<ApiResponse<String>> syncAllData() {
        log.info("Manual sync triggered");
        dataSyncService.syncAllData();
        return ResponseEntity.ok(ApiResponse.success("Data sync completed successfully", null));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<String>> syncCategories() {
        log.info("Manual category sync triggered");
        dataSyncService.syncCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories synced successfully", null));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<String>> syncProducts() {
        log.info("Manual product sync triggered");
        dataSyncService.syncProducts();
        return ResponseEntity.ok(ApiResponse.success("Products synced successfully", null));
    }
}
