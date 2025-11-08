package com.restaurant.store.dto.admin.response;

import com.restaurant.store.dto.admin.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    
    private String token;
    private UserDTO user;
    
    public static TokenResponse of(String token, UserDTO user) {
        return TokenResponse.builder()
                .token(token)
                .user(user)
                .build();
    }
}