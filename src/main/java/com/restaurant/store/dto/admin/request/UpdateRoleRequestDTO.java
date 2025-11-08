package com.restaurant.store.dto.admin.request;

import com.restaurant.store.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequestDTO {
    @NotNull(message = "Role is required")
    private Role role;
}