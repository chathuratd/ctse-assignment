package com.ctse.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtUtils — JWT Generation and Validation
 * ==========================================
 *
 * HOW JWT WORKS (the full flow):
 * ──────────────────────────────
 *
 * 1. USER LOGS IN → POST /auth/login with email + password
 * 2. Server verifies password with BCrypt
 * 3. Server calls generateToken(user) → returns a signed JWT string
 * 4. Client stores the token (localStorage, cookie, etc.)
 *
 * 5. USER MAKES PROTECTED REQUEST → GET /api/users/me
 *    Header: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 * 6. JwtAuthenticationFilter intercepts the request
 * 7. Filter calls extractUsername() → gets "user@email.com" from token
 * 8. Filter calls isTokenValid() → verifies signature + not expired
 * 9. If valid → sets Authentication in SecurityContext → request proceeds
 * 10. If invalid → returns 401 Unauthorized
 *
 * TOKEN STRUCTURE:
 * ────────────────
 * eyJhbGciOiJIUzI1NiJ9         ← Header (Base64)  {"alg":"HS256"}
 * .
 * eyJzdWIiOiJ1c2VyQHRlc3QuY29t ← Payload (Base64) {"sub":"user@test.com","role":"ROLE_USER","iat":...,"exp":...}
 * .
 * SflKxwRJSMeKKF2QT4fwpMeJf36P  ← Signature (HMAC-SHA256 of header+payload using secret key)
 *
 * SIGNING KEY:
 * ────────────
 * - Stored in application.yml as a Base64 string
 * - Only the server knows this key
 * - Any tampering with the token will invalidate the signature
 * - For production: use a minimum 256-bit (32-byte) key
 *
 * @Component → Spring manages this as a singleton bean (created once, reused)
 * @Value     → Spring injects values from application.yml
 */
@Component
public class JwtUtils {

    // Injected from application.yml: app.jwt.secret
    // This is the secret key used to sign and verify tokens
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Injected from application.yml: app.jwt.expiration-ms
    // Default: 86400000 ms = 24 hours
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Key helper ────────────────────────────────────────────────────────────

    /**
     * Converts the Base64 secret string into a cryptographic Key object.
     * Keys.hmacShaKeyFor() ensures the key meets the HMAC-SHA256 requirements.
     */
    private Key getSigningKey() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token Generation ──────────────────────────────────────────────────────

    /**
     * Generates a JWT token for the given user.
     *
     * Claims embedded in the token payload:
     *   sub  = subject = email (standard JWT claim — identifies the user)
     *   role = the user's role (custom claim — used for authorization in other services)
     *   iat  = issued at (auto-set by JJWT)
     *   exp  = expiration time
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Add the role as a custom claim so other services can read it from the token
        extraClaims.put("role", userDetails.getAuthorities()
                .iterator().next().getAuthority());
        return buildToken(extraClaims, userDetails);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())          // email
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Token Validation ──────────────────────────────────────────────────────

    /**
     * Validates a token:
     * 1. Checks username in token matches the expected user
     * 2. Checks the token has not expired
     * 3. JJWT automatically verifies the signature during parsing
     *    (throws JwtException if tampered or malformed)
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Token is malformed, tampered, or has an invalid signature
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Claims Extraction ─────────────────────────────────────────────────────

    /**
     * Extracts the subject (email) from the token.
     * Called by the authentication filter on every incoming request.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claims extractor — takes a function that maps Claims → T.
     * Example: extractClaim(token, Claims::getSubject) → email string
     *          extractClaim(token, Claims::getExpiration) → Date
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and validates the token signature, then returns all claims.
     * JJWT throws SignatureException if the token has been tampered with.
     * JJWT throws ExpiredJwtException if the token has expired.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
