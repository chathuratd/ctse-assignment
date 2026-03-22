package com.ctse.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateConfig
 * ==================
 * Configures a RestTemplate bean so UserService can call Notification Service.
 *
 * User Service → Notification Service:
 *   GET /users/{id}/notifications
 *     internally calls → GET http://notification-service:8004/notifications/user/{userId}
 *
 * This makes User Service an ACTIVE participant in inter-service communication,
 * not just a passive receiver of calls.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(5_000);
        return new RestTemplate(factory);
    }
}
