package com.ctse.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter
 * ========================
 * This filter runs ONCE per HTTP request (OncePerRequestFilter).
 * It intercepts every request and checks if a valid JWT is present.
 *
 * FILTER CHAIN CONCEPT:
 * ─────────────────────
 * Spring Security processes requests through a chain of filters.
 * Each filter can choose to:
 *   a) Process the request and pass it forward (filterChain.doFilter)
 *   b) Block the request and return an error response
 *
 * Our filter sits early in the chain and sets the Authentication
 * object in the SecurityContext if the JWT is valid.
 *
 * WHAT IS SecurityContext?
 * ─────────────────────────
 * The SecurityContext is a thread-local storage that holds the
 * Authentication object for the current request's thread.
 * Spring Security checks this context to decide if a request is authorized.
 *
 * If SecurityContext has a valid Authentication → request is AUTHENTICATED
 * If SecurityContext is empty → request is treated as ANONYMOUS
 *
 * FLOW:
 * ─────
 * Request comes in
 *   → Extract "Authorization" header
 *   → Check if it starts with "Bearer "
 *   → Extract JWT token (everything after "Bearer ")
 *   → Extract email (username) from token
 *   → Load UserDetails from DB using the email
 *   → Validate token against UserDetails
 *   → If valid: set Authentication in SecurityContext
 *   → Pass request to next filter in chain
 *
 * @RequiredArgsConstructor → Lombok: generates a constructor for all final fields
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Read the Authorization header from the request
        final String authHeader = request.getHeader("Authorization");

        // 2. If no token present, skip this filter (treat as anonymous request)
        //    SecurityConfig will handle the 401 for protected endpoints
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the JWT (strip the "Bearer " prefix — 7 characters)
        final String jwt = authHeader.substring(7);

        // 4. Extract email (username) from the JWT payload
        final String userEmail = jwtUtils.extractUsername(jwt);

        // 5. Only proceed if we have an email AND the user isn't already authenticated
        //    SecurityContextHolder.getContext().getAuthentication() == null means
        //    no authentication has been set for this request yet
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Load the full UserDetails from the database
            //    This ensures the user still exists (wasn't deleted after token was issued)
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // 7. Validate the token against the loaded UserDetails
            if (jwtUtils.isTokenValid(jwt, userDetails)) {

                // 8. Create an Authentication object
                //    UsernamePasswordAuthenticationToken is Spring Security's standard
                //    authentication object. Parameters:
                //      principal   = the UserDetails object (who is this user)
                //      credentials = null (we don't need the password anymore, token is proof)
                //      authorities = the user's roles/permissions
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Add extra request metadata (IP address, session ID, etc.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 9. SET the Authentication in the SecurityContext
                //    From this point forward, Spring Security knows who the user is
                //    and what they're allowed to do for this request
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 10. Pass the request to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}
