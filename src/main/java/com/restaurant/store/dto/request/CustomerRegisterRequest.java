package com.restaurant.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer registration request payload")
public class CustomerRegisterRequest {
    
    @Schema(description = "Customer's full name", example = "John Doe", required = true)
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;
    
    @Schema(description = "Customer's email address", example = "john.doe@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 150, message = "Email must be less than 150 characters")
    private String email;
    
    @Schema(description = "Customer's phone number", example = "+1234567890")
    @Size(max = 20, message = "Phone must be less than 20 characters")
    private String phone;
    
    @Schema(description = "Customer's password (min 6 characters)", example = "SecurePass123!", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String password;
    
    @Schema(description = "Customer's delivery address", example = "123 Main St, Apt 4B, New York, NY 10001")
    @Size(max = 255, message = "Address must be less than 255 characters")
    private String address;
}