package com.ctse.registrationservice.dto;

import lombok.Data;

/**
 * UserResponse (Mirror DTO)
 * ==========================
 * Used for deserializing the response from User Service.
 *
 * When Registration Service calls:
 *   GET http://user-service:8001/users/{userId}
 *
 * User Service returns this JSON:
 *   { "id": "uuid", "name": "John Doe", "email": "john@example.com" }
 *
 * RestTemplate maps the JSON response into this class.
 * We only declare the fields we need — Jackson ignores unknown fields
 * (due to spring.jackson.deserialization.fail-on-unknown-properties=false,
 * which is the Spring Boot default).
 *
 * WHY A MIRROR DTO?
 * ──────────────────
 * We do NOT import user-service's UserResponse class here.
 * That would create a compile-time dependency between services — exactly what
 * microservices are designed to avoid. Each service is independently deployable
 * and should not share code with other services.
 */
@Data
public class UserResponse {
    private String id;
    private String name;
    private String email;
}
