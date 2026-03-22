package com.ctse.userservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler
 * ======================
 * Centralized exception handling for all controllers.
 *
 * @RestControllerAdvice
 *   = @ControllerAdvice + @ResponseBody
 *   Intercepts exceptions thrown by any @RestController
 *   and converts them to JSON error responses.
 *
 * WHY IS THIS IMPORTANT?
 * ──────────────────────
 * Without this, Spring Boot would return a generic error page or
 * expose internal stack trace details. This gives you:
 * 1. Clean, consistent JSON error format for all errors
 * 2. Correct HTTP status codes (400, 401, 403, 404, 500)
 * 3. Security: no internal details leaked to clients
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Valid validation failures.
     * Triggered when RegisterRequest or LoginRequest fields fail validation.
     *
     * Returns 400 with field-level error messages:
     * {
     *   "status": 400,
     *   "errors": {
     *     "email": "Email must be valid",
     *     "password": "Password must be at least 8 characters"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(fieldName, message);
        });

        Map<String, Object> body = new HashMap<>();
        body.put("status", 400);
        body.put("error", "Validation Failed");
        body.put("errors", fieldErrors);
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles wrong password during login.
     * Spring Security throws BadCredentialsException when credentials are wrong.
     * We catch it and return a generic message (don't say "wrong password" or
     * "email not found" separately — that helps attackers enumerate accounts).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials() {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    /**
     * Handles duplicate email during registration.
     * Thrown by AuthService when email already exists.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles user not found (404).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().startsWith("User not found")) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
