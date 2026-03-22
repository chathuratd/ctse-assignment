package com.ctse.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * UserServiceApplication — Spring Boot Entry Point
 * ==================================================
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   @Configuration     → This class can define @Bean methods
 *   @EnableAutoConfiguration → Spring Boot auto-configures beans based on classpath
 *                               (e.g., sees postgresql + JPA → auto-configures DataSource)
 *   @ComponentScan     → Scans com.ctse.userservice and all sub-packages for
 *                         @Component, @Service, @Repository, @Controller beans
 *
 * Spring Boot Auto-Configuration:
 * ──────────────────────────────────
 * When you add a dependency to pom.xml, Spring Boot's auto-configuration
 * reads it and sets up the necessary beans automatically.
 * Example:
 *   - spring-boot-starter-data-jpa + postgresql driver + DB config in yml
 *     → Spring Boot automatically creates: DataSource, EntityManagerFactory,
 *       TransactionManager, JPA repositories
 *   You don't write any of that boilerplate code!
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
