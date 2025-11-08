package com.restaurant.store.dto.admin;

import com.resadmin.res.entity.User;
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
    private User.Role role;
}