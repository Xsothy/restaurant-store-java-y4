package com.restaurant.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private String clientSecret;
    private String sessionUrl;
    private String successUrl;
    private String cancelUrl;
    private String type;
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (paymentId != null) map.put("paymentIntentId", paymentId);
        if (clientSecret != null) map.put("clientSecret", clientSecret);
        if (sessionUrl != null) map.put("sessionUrl", sessionUrl);
        if (successUrl != null) map.put("successUrl", successUrl);
        if (cancelUrl != null) map.put("cancelUrl", cancelUrl);
        if (type != null) map.put("type", type);
        return map;
    }
}
