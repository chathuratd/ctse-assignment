package com.ctse.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LoginRequest DTO
 * ================
 * What the client sends when calling:
 *   POST /auth/login
 *
 * All we need is email + password.
 * The service layer will verify the password against the BCrypt hash in the DB,
 * then generate and return a JWT token.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
