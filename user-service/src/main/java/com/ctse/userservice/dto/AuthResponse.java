package com.ctse.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthResponse DTO
 * ================
 * What the server returns after a successful login or registration.
 *
 * What is a JWT (JSON Web Token)?
 * --------------------------------
 * A JWT is a self-contained token made of 3 Base64-encoded parts:
 *   HEADER.PAYLOAD.SIGNATURE
 *
 * Header:  {"alg": "HS256", "typ": "JWT"}
 * Payload: {"sub": "user@email.com", "role": "ROLE_USER", "iat": ..., "exp": ...}
 * Signature: HMAC_SHA256(header + "." + payload, secretKey)
 *
 * Why is this good for microservices?
 * ------------------------------------
 * - STATELESS: The server doesn't store sessions. The token IS the proof.
 * - SELF-CONTAINED: The payload carries user identity + role.
 * - ANY SERVICE can validate the token using the shared secret key.
 *   (No database lookup needed on every request — just verify the signature)
 *
 * Token type:
 * -----------
 * "Bearer" is a standard prefix used in the Authorization header:
 *   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 *
 * expiresIn: how many seconds until the token expires (e.g., 86400 = 24 hours)
 * After expiry, the client must log in again to get a new token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;   // in seconds
    private String userId;    // UUID as string
    private String email;
    private String name;
    private String role;
}
