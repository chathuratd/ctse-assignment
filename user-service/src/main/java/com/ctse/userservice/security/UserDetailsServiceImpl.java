package com.ctse.userservice.security;

import com.ctse.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsServiceImpl
 * ======================
 * Implements Spring Security's UserDetailsService interface.
 *
 * WHY DO WE NEED THIS?
 * ─────────────────────
 * Spring Security doesn't know anything about your database.
 * It only works with UserDetails objects.
 *
 * YOUR JOB: implement loadUserByUsername() to:
 *   1. Look up the user in your database by their "username" (email in our case)
 *   2. Return the UserDetails object (our User entity already implements UserDetails)
 *
 * WHERE is this called?
 * ──────────────────────
 * 1. By DaoAuthenticationProvider during login
 *    → "Give me the user with this email, I'll check the password"
 *
 * 2. By JwtAuthenticationFilter on every protected request
 *    → "Give me the user with this email so I can validate their token"
 *
 * @Service → Registers this as a Spring bean in the application context
 * @RequiredArgsConstructor → Lombok generates a constructor injecting userRepository
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by their "username" (which for us is their email address).
     *
     * @param username the email address                ← Spring Security calls this "username"
     * @return UserDetails (our User entity, since it implements UserDetails)
     * @throws UsernameNotFoundException if no user with that email exists in the DB
     *
     * Note: UsernameNotFoundException is a Spring Security exception.
     * Spring Security catches it during login and returns 401 Unauthorized.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username
                ));
    }
}
