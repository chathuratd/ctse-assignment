package com.ctse.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * RegisterRequest DTO
 * ===================
 * This is what the client sends in the request body when calling:
 *   POST /auth/register
 *
 * Why a separate DTO instead of using the User entity directly?
 * -------------------------------------------------------------
 * 1. SECURITY: The User entity has a 'role' field. If you accepted it
 *    directly, a malicious user could register themselves as ROLE_ADMIN.
 *    The DTO only exposes what the client is ALLOWED to set.
 *
 * 2. VALIDATION: @NotBlank, @Email, @Size enforce rules at the HTTP layer
 *    before any business logic runs.
 *
 * 3. DECOUPLING: Your API contract is separate from your DB schema.
 *    You can change the DB without changing the API and vice versa.
 *
 * Validation annotations (from spring-boot-starter-validation):
 *   @NotBlank  → field must not be null or whitespace-only
 *   @Email     → must be a valid email format
 *   @Size      → min/max character length
 *
 * These are triggered by @Valid on the controller method parameter.
 */
@Data
public class RegisterRequest {

    // Field is called 'name' to match the system API spec: POST /users { "name": "..." }
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
