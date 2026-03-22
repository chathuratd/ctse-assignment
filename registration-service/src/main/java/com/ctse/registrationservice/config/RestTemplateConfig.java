package com.ctse.registrationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplateConfig
 * ==================
 * Configures a RestTemplate bean for inter-service HTTP communication.
 *
 * WHAT IS RestTemplate?
 * ──────────────────────
 * RestTemplate is Spring's synchronous HTTP client.
 * It sends HTTP requests and deserializes JSON responses into Java objects.
 *
 * Example usage in RegistrationService:
 *   UserResponse user = restTemplate.getForObject(
 *       "http://user-service:8001/users/" + userId,
 *       UserResponse.class
 *   );
 *
 * WHY @Bean?
 * ───────────
 * We define it as a Spring @Bean so it can be injected anywhere via:
 *   @Autowired private RestTemplate restTemplate;
 *
 * Timeouts (via SimpleClientHttpRequestFactory):
 *   connectTimeout → ms to wait to establish TCP connection (5 000 ms)
 *   readTimeout    → ms to wait for the response once connected (5 000 ms)
 *   Both set to fail fast if a downstream service is down.
 *
 * NOTE on RestTemplateBuilder API change:
 * In Spring Boot 3.2, connectTimeout(Duration) / readTimeout(Duration) were
 * removed from RestTemplateBuilder. The correct approach is to configure
 * timeouts on the underlying ClientHttpRequestFactory directly.
 *
 * ALTERNATIVE: WebClient (reactive, non-blocking)
 * In modern Spring Boot, WebClient is preferred over RestTemplate
 * for its reactive/async support. For this learning project,
 * RestTemplate is simpler and easier to understand.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // SimpleClientHttpRequestFactory wraps Java's HttpURLConnection
        // and is the default factory used by RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);   // 5 seconds
        factory.setReadTimeout(5_000);      // 5 seconds
        return new RestTemplate(factory);
    }
}
