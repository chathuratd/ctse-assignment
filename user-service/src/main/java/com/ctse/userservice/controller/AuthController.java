package com.ctse.userservice.controller;

import com.ctse.userservice.dto.AuthResponse;
import com.ctse.userservice.dto.LoginRequest;
import com.ctse.userservice.dto.RegisterRequest;
import com.ctse.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — Public REST Endpoints for Authentication
 * ==========================================================
 *
 * Base path: /auth
 * These endpoints are PUBLIC — listed in SecurityConfig.permitAll()
 * No JWT token is required to access them.
 *
 * Endpoints:
 *   POST /auth/register  → Create a new user account
 *   POST /auth/login     → Login and receive a JWT token
 *
 * ─────────────────────────────────────────────────────────
 * ANNOTATIONS EXPLAINED:
 *
 * @RestController
 *   = @Controller + @ResponseBody
 *   Every method return value is automatically serialized to JSON
 *   (using Jackson, which Spring Boot includes by default)
 *
 * @RequestMapping("/auth")
 *   All methods in this class are prefixed with /auth
 *
 * @RequiredArgsConstructor
 *   Lombok: generates constructor-based dependency injection for final fields
 *
 * @Valid
 *   Triggers Jakarta Bean Validation on the @RequestBody DTO
 *   (checks @NotBlank, @Email, @Size annotations on RegisterRequest/LoginRequest)
 *   If validation fails → 400 Bad Request with error details
 *
 * ResponseEntity<T>
 *   Gives you full control over HTTP response: status code, headers, body
 *   ResponseEntity.ok(body)           → 200 OK
 *   ResponseEntity.status(201).body() → 201 Created
 * ─────────────────────────────────────────────────────────
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /users  (Primary registration endpoint — matches system API spec)
     *
     * Request body: { "name": "John Doe", "email": "...", "password": "..." }
     * Response (201 Created): JWT token + user info
     */
    @PostMapping("/users")
    public ResponseEntity<AuthResponse> registerViaUsersPath(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * POST /auth/register  (Kept for backward compatibility)
     *
     *
     * Request body (JSON):
     * {
     *   "fullName": "John Doe",
     *   "email": "john@university.edu",
     *   "password": "securePass123"
     * }
     *
     * Response (201 Created):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 86400,
     *   "userId": 1,
     *   "email": "john@university.edu",
     *   "fullName": "John Doe",
     *   "role": "ROLE_USER"
     * }
     */
    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /auth/login
     *
     * Request body (JSON):
     * {
     *   "email": "john@university.edu",
     *   "password": "securePass123"
     * }
     *
     * Response (200 OK):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiJ9...",
     *   ...
     * }
     */
    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
