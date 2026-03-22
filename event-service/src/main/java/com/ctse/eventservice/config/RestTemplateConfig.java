package com.ctse.eventservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateConfig — HTTP client bean for inter-service communication.
 *
 * Event Service calls:
 *   User Service         → GET /users          (list all users when a new event is created)
 *   Notification Service → POST /notifications (send NEW_EVENT notification per user)
 *
 * NOTE: RestTemplateBuilder.connectTimeout(Duration) was removed in Spring Boot 3.2.
 * We use SimpleClientHttpRequestFactory with millisecond integers directly.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
