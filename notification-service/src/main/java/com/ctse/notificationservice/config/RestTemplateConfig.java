package com.ctse.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateConfig — HTTP client bean for inter-service communication.
 *
 * Notification Service calls:
 *   User Service  → GET /users/{userId}  (fetch name/email for message templates)
 *   Event Service → GET /events/{eventId} (fetch title/location/date for templates)
 *
 * We use SimpleClientHttpRequestFactory with explicit timeouts to avoid
 * hanging threads when a downstream service is slow or unresponsive.
 * connectTimeout: max time to establish the TCP connection.
 * readTimeout:    max time to wait for the first byte of response.
 *
 * NOTE: RestTemplateBuilder.connectTimeout(Duration) was removed in
 * Spring Boot 3.2. We use SimpleClientHttpRequestFactory directly.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5 seconds to connect
        factory.setReadTimeout(5000);      // 5 seconds to read response
        return new RestTemplate(factory);
    }
}
