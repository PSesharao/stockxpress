package com.seshrao.stockxpress.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for API Gateway SecurityConfig.
 * Tests security configuration for OAuth2 resource server with JWT.
 * Covers endpoint security, CSRF protection, and authentication requirements.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.gateway.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8181/realms/stockxpress"
})
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private SecurityWebFilterChain securityWebFilterChain;

    /**
     * Test that SecurityConfig bean is created and loaded.
     * Given: Spring context with SecurityConfig
     * When: Context is initialized
     * Then: SecurityConfig bean is not null
     */
    @Test
    @DisplayName("Should load SecurityConfig bean")
    void shouldLoadSecurityConfigBean() {
        // Then
        assertThat(securityConfig).isNotNull();
    }

    /**
     * Test that SecurityWebFilterChain bean is created.
     * Given: Spring context with security configuration
     * When: Context is initialized
     * Then: SecurityWebFilterChain bean is not null
     */
    @Test
    @DisplayName("Should create SecurityWebFilterChain bean")
    void shouldCreateSecurityWebFilterChainBean() {
        // Then
        assertThat(securityWebFilterChain).isNotNull();
    }

    /**
     * Test that /eureka/** endpoints are publicly accessible without authentication.
     * Given: Security configuration with eureka path permitted
     * When: Accessing /eureka/web endpoint without authentication
     * Then: Request is permitted (returns 404 as endpoint doesn't exist, not 401)
     */
    @Test
    @DisplayName("Should permit access to /eureka/** endpoints without authentication")
    void shouldPermitAccessToEurekaEndpointsWithoutAuth() {
        // When & Then
        webTestClient.get()
                .uri("/eureka/web")
                .exchange()
                .expectStatus().isNotFound(); // 404 means it passed security, endpoint just doesn't exist
    }

    /**
     * Test that /eureka/css endpoint is accessible without authentication.
     * Given: Security configuration with eureka path permitted
     * When: Accessing /eureka/css/style.css without authentication
     * Then: Request is permitted (404, not 401)
     */
    @Test
    @DisplayName("Should permit access to /eureka/css/** for static resources")
    void shouldPermitAccessToEurekaCssWithoutAuth() {
        // When & Then
        webTestClient.get()
                .uri("/eureka/css/style.css")
                .exchange()
                .expectStatus().isNotFound(); // Passes security, but resource doesn't exist
    }

    /**
     * Test that /eureka/js endpoint is accessible without authentication.
     * Given: Security configuration with eureka path permitted
     * When: Accessing /eureka/js/app.js without authentication
     * Then: Request is permitted (404, not 401)
     */
    @Test
    @DisplayName("Should permit access to /eureka/js/** for static resources")
    void shouldPermitAccessToEurekaJsWithoutAuth() {
        // When & Then
        webTestClient.get()
                .uri("/eureka/js/app.js")
                .exchange()
                .expectStatus().isNotFound(); // Passes security check
    }

    /**
     * Test that non-eureka endpoints require authentication.
     * Given: Security configuration requiring authentication for other paths
     * When: Accessing /api/product without authentication
     * Then: Request is unauthorized (401)
     */
    @Test
    @DisplayName("Should require authentication for non-eureka endpoints")
    void shouldRequireAuthenticationForNonEurekaEndpoints() {
        // When & Then
        webTestClient.get()
                .uri("/api/product")
                .exchange()
                .expectStatus().isUnauthorized(); // 401 Unauthorized
    }

    /**
     * Test that root endpoint requires authentication.
     * Given: Security configuration requiring authentication
     * When: Accessing / without authentication
     * Then: Request is unauthorized (401)
     */
    @Test
    @DisplayName("Should require authentication for root endpoint")
    void shouldRequireAuthenticationForRootEndpoint() {
        // When & Then
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test that /api/order endpoint requires authentication.
     * Given: Security configuration requiring authentication
     * When: Accessing /api/order without authentication
     * Then: Request is unauthorized (401)
     */
    @Test
    @DisplayName("Should require authentication for /api/order endpoint")
    void shouldRequireAuthenticationForOrderEndpoint() {
        // When & Then
        webTestClient.post()
                .uri("/api/order")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test that /api/inventory endpoint requires authentication.
     * Given: Security configuration requiring authentication
     * When: Accessing /api/inventory without authentication
     * Then: Request is unauthorized (401)
     */
    @Test
    @DisplayName("Should require authentication for /api/inventory endpoint")
    void shouldRequireAuthenticationForInventoryEndpoint() {
        // When & Then
        webTestClient.get()
                .uri("/api/inventory")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test CSRF is disabled for API gateway.
     * Given: Security configuration with CSRF disabled
     * When: Making POST request without CSRF token
     * Then: Request is rejected only due to authentication, not CSRF
     */
    @Test
    @DisplayName("Should have CSRF protection disabled")
    void shouldHaveCsrfDisabled() {
        // When & Then - POST without CSRF token should fail with 401 (auth), not 403 (CSRF)
        webTestClient.post()
                .uri("/api/order")
                .exchange()
                .expectStatus().isUnauthorized(); // 401, not 403 CSRF error
    }

    /**
     * Test that PUT requests require authentication.
     * Given: Security configuration
     * When: Making PUT request without authentication
     * Then: Request is unauthorized
     */
    @Test
    @DisplayName("Should require authentication for PUT requests")
    void shouldRequireAuthenticationForPutRequests() {
        // When & Then
        webTestClient.put()
                .uri("/api/product/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test that DELETE requests require authentication.
     * Given: Security configuration
     * When: Making DELETE request without authentication
     * Then: Request is unauthorized
     */
    @Test
    @DisplayName("Should require authentication for DELETE requests")
    void shouldRequireAuthenticationForDeleteRequests() {
        // When & Then
        webTestClient.delete()
                .uri("/api/product/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Test security filter chain configuration with ServerHttpSecurity.
     * Given: SecurityConfig class with securityWebFilterChain method
     * When: Creating SecurityWebFilterChain with ServerHttpSecurity
     * Then: Filter chain is built correctly with OAuth2 JWT configuration
     */
    @Test
    @DisplayName("Should configure OAuth2 resource server with JWT")
    void shouldConfigureOAuth2ResourceServerWithJwt() {
        // Given
        ServerHttpSecurity http = ServerHttpSecurity.http();

        // When
        SecurityWebFilterChain filterChain = securityConfig.securityWebFilterChain(http);

        // Then
        assertThat(filterChain).isNotNull();
        assertThat(filterChain.getWebFilters()).isNotEmpty();
    }

    /**
     * Test that /eureka/main endpoint is accessible.
     * Given: Eureka endpoints are permitted
     * When: Accessing /eureka/main without authentication
     * Then: Request passes security (404, not 401)
     */
    @Test
    @DisplayName("Should permit access to /eureka/main endpoint")
    void shouldPermitAccessToEurekaMain() {
        // When & Then
        webTestClient.get()
                .uri("/eureka/main")
                .exchange()
                .expectStatus().isNotFound();
    }

    /**
     * Test that deeply nested eureka paths are accessible.
     * Given: Eureka /** pattern is permitted
     * When: Accessing /eureka/apps/registry without authentication
     * Then: Request passes security
     */
    @Test
    @DisplayName("Should permit access to deeply nested eureka paths")
    void shouldPermitAccessToDeeplyNestedEurekaPaths() {
        // When & Then
        webTestClient.get()
                .uri("/eureka/apps/registry/service")
                .exchange()
                .expectStatus().isNotFound(); // Passes security
    }
}
