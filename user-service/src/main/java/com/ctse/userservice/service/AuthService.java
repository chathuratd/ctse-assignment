package com.ctse.userservice.service;

import com.ctse.userservice.dto.AuthResponse;
import com.ctse.userservice.dto.LoginRequest;
import com.ctse.userservice.dto.RegisterRequest;
import com.ctse.userservice.model.Role;
import com.ctse.userservice.model.User;
import com.ctse.userservice.repository.UserRepository;
import com.ctse.userservice.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * AuthService — Registration and Login Business Logic
 * =====================================================
 *
 * REGISTRATION FLOW:
 * ──────────────────
 * 1. Check if email already exists in DB → if yes, throw exception
 * 2. Hash the password with BCrypt
 * 3. Save the new User to DB (with ROLE_USER by default)
 * 4. Send a WELCOME notification via Notification Service (non-blocking)
 * 5. Generate a JWT token for the new user
 * 6. Return AuthResponse with the token + user info
 *
 * INTER-SERVICE CALL (step 4):
 * ─────────────────────────────
 * User Service → Notification Service
 * POST http://notification-service:8004/notifications
 * Body: { "type": "WELCOME", "userId": "<new-user-uuid>" }
 *
 * Notification Service will call back to GET /users/{userId} to look up
 * the user's name and build the welcome message — that's how the template
 * engine works on the notification side.
 *
 * LOGIN FLOW:
 * ────────────
 * 1. Call authenticationManager.authenticate()
 * 2. Load the User entity from DB
 * 3. Generate a JWT token
 * 4. Return AuthResponse with the token + user info
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final RestTemplate restTemplate;

    @Value("${app.services.notification-url}")
    private String notificationServiceUrl;

    public AuthResponse register(RegisterRequest request) {
        // 1. Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                "An account with email '" + request.getEmail() + "' already exists."
            );
        }

        // 2. Build the User entity
        User user = User.builder()
                .fullName(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .build();

        // 3. Persist to database
        User savedUser = userRepository.save(user);

        // 4. Send WELCOME notification (non-critical — failure does not block registration)
        sendWelcomeNotification(savedUser.getId().toString());

        // 5. Generate JWT for immediate login after registration
        String token = jwtUtils.generateToken(savedUser);

        // 6. Build and return the response
        return buildAuthResponse(savedUser, token);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String token = jwtUtils.generateToken(user);
        return buildAuthResponse(user, token);
    }

    // ── Inter-service call ────────────────────────────────────────────────────

    /**
     * POST /notifications to Notification Service with type=WELCOME.
     * Notification Service will fetch the user's name and build the message.
     * Non-critical: we log the error but never fail a registration because of it.
     */
    private void sendWelcomeNotification(String userId) {
        String url = notificationServiceUrl + "/notifications";
        try {
            Map<String, String> body = Map.of("type", "WELCOME", "userId", userId);
            restTemplate.postForObject(url, body, Object.class);
            log.info("Welcome notification dispatched for user {}", userId);
        } catch (RestClientException e) {
            log.warn("Could not send welcome notification for user {} (non-critical): {}",
                    userId, e.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpirationMs() / 1000)
                .userId(user.getId() != null ? user.getId().toString() : null)
                .email(user.getEmail())
                .name(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
