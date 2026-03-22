package com.ctse.userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — The Heart of Spring Security
 * ===============================================
 *
 * @Configuration    → This class provides Spring @Bean definitions
 * @EnableWebSecurity → Activates Spring Security's web security support
 * @EnableMethodSecurity → Enables @PreAuthorize annotations on controller methods
 *                         (e.g., only ADMIN can call a method)
 *
 * KEY CONCEPTS EXPLAINED:
 * ────────────────────────
 *
 * 1. CSRF (Cross-Site Request Forgery) — DISABLED here
 *    CSRF protection is for browser-based applications that use session cookies.
 *    Our API is stateless (JWT, no sessions) and doesn't use cookies,
 *    so CSRF protection is unnecessary and would break our API.
 *
 * 2. SESSION MANAGEMENT — STATELESS
 *    Traditional web apps store sessions on the server (STATEFUL).
 *    We're STATELESS: each request is self-contained with its JWT.
 *    Pros of stateless: scales horizontally (any server can handle any request),
 *                       no shared session store needed in distributed systems.
 *
 * 3. PASSWORD ENCODER — BCrypt
 *    BCrypt is a one-way hashing algorithm designed for passwords.
 *    Features:
 *      - SALT: adds random bytes before hashing (same password → different hash each time)
 *      - WORK FACTOR: configurable rounds (default 10) — makes brute force expensive
 *      - SLOW BY DESIGN: unlike MD5/SHA, BCrypt is intentionally slow
 *    NEVER store plain text passwords. NEVER use MD5/SHA for passwords.
 *
 * 4. AuthenticationProvider
 *    Tells Spring Security HOW to authenticate users:
 *    "Load user by email from DB, then compare BCrypt hash with submitted password"
 *
 * 5. AuthenticationManager
 *    The entry point for authentication. Called by AuthService during login.
 *    It delegates to our AuthenticationProvider.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * SecurityFilterChain — defines the security rules for HTTP requests.
     * This is the MAIN security configuration. Every request goes through this.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Disable CSRF (not needed for stateless JWT APIs) ──────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Authorization Rules ───────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // PUBLIC — user registration and login
                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                .requestMatchers("/auth/**").permitAll()

                // PUBLIC — health check for AWS ECS/ALB
                .requestMatchers("/actuator/**").permitAll()

                // PUBLIC — inter-service endpoints (no JWT from other services)
                // GET /users/{id} — called by Registration Service and Notification Service
                // GET /users      — called by Event Service to get all users for NEW_EVENT notifications
                // In production these are restricted at the network/VPC level.
                .requestMatchers(HttpMethod.GET, "/users/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/users").permitAll()

                // ADMIN-only
                .requestMatchers("/admin/users").hasAuthority("ROLE_ADMIN")

                // All other endpoints require a valid JWT
                .anyRequest().authenticated()
            )

            // ── Stateless session (no server-side sessions) ───────────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── Authentication provider ───────────────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── Register our JWT filter BEFORE Spring's default login filter
            // This ensures JWT is checked before any form-based auth logic
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCryptPasswordEncoder — the password hashing strategy.
     *
     * The strength parameter (10) is the number of hashing rounds (2^10 = 1024).
     * Higher = more secure but slower. 10-12 is the industry standard.
     *
     * Spring Security uses this bean automatically when you call
     * passwordEncoder.encode(rawPassword) or passwordEncoder.matches(raw, encoded).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * DaoAuthenticationProvider links Spring Security to our database.
     * "Dao" = Data Access Object — it loads users from the data source.
     *
     * When login() is called, Spring Security:
     *   1. Calls userDetailsService.loadUserByUsername(email) → loads User from DB
     *   2. Calls passwordEncoder.matches(rawPassword, storedHash) → verifies password
     *   3. Returns an Authentication object if successful
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager — the entry point that AuthService calls during login.
     * Spring manages this automatically based on the AuthenticationConfiguration.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
