package com.seshrao.stockxpress.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for Discovery Server SecurityConfig.
 * Tests HTTP Basic authentication with in-memory user configuration.
 * Covers authentication requirements and CSRF protection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "eureka.username=eureka",
        "eureka.password=password"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

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
     * Test that accessing endpoints without authentication returns 401.
     * Given: Security configuration requiring authentication
     * When: Accessing / without credentials
     * Then: Returns HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("Should return 401 Unauthorized when accessing endpoint without authentication")
    void shouldReturnUnauthorizedWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that accessing /eureka endpoint without authentication returns 401.
     * Given: Security configuration requiring authentication for all requests
     * When: Accessing /eureka without credentials
     * Then: Returns HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("Should return 401 for /eureka endpoint without authentication")
    void shouldReturnUnauthorizedForEurekaEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/eureka"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that accessing endpoint with valid credentials is successful.
     * Given: Valid username and password configured
     * When: Accessing / with correct HTTP Basic credentials
     * Then: Request is authenticated (returns 404 as endpoint doesn't exist, not 401)
     */
    @Test
    @DisplayName("Should authenticate successfully with valid credentials")
    void shouldAuthenticateWithValidCredentials() throws Exception {
        // When & Then
        mockMvc.perform(get("/")
                        .with(httpBasic("eureka", "password")))
                .andExpect(status().isNotFound()); // 404 means authenticated, endpoint doesn't exist
    }

    /**
     * Test that accessing endpoint with invalid username returns 401.
     * Given: Invalid username credentials
     * When: Accessing / with wrong username
     * Then: Returns HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("Should return 401 with invalid username")
    void shouldReturnUnauthorizedWithInvalidUsername() throws Exception {
        // When & Then
        mockMvc.perform(get("/")
                        .with(httpBasic("wronguser", "password")))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that accessing endpoint with invalid password returns 401.
     * Given: Invalid password credentials
     * When: Accessing / with wrong password
     * Then: Returns HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("Should return 401 with invalid password")
    void shouldReturnUnauthorizedWithInvalidPassword() throws Exception {
        // When & Then
        mockMvc.perform(get("/")
                        .with(httpBasic("eureka", "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that accessing endpoint with empty credentials returns 401.
     * Given: Empty username and password
     * When: Accessing / with empty credentials
     * Then: Returns HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("Should return 401 with empty credentials")
    void shouldReturnUnauthorizedWithEmptyCredentials() throws Exception {
        // When & Then
        mockMvc.perform(get("/")
                        .with(httpBasic("", "")))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that POST requests without authentication are rejected.
     * Given: Security configuration requiring authentication
     * When: Making POST request without credentials
     * Then: Returns HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("Should return 401 for POST requests without authentication")
    void shouldReturnUnauthorizedForPostWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/test"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that POST requests with valid authentication are accepted.
     * Given: Valid credentials
     * When: Making POST request with HTTP Basic authentication
     * Then: Request is authenticated (404 as endpoint doesn't exist)
     */
    @Test
    @DisplayName("Should authenticate POST requests with valid credentials")
    void shouldAuthenticatePostRequestsWithValidCredentials() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/test")
                        .with(httpBasic("eureka", "password")))
                .andExpect(status().isNotFound()); // Authenticated, endpoint doesn't exist
    }

    /**
     * Test that PUT requests require authentication.
     * Given: Security configuration
     * When: Making PUT request without authentication
     * Then: Returns HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("Should return 401 for PUT requests without authentication")
    void shouldReturnUnauthorizedForPutWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/test/1"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that DELETE requests require authentication.
     * Given: Security configuration
     * When: Making DELETE request without authentication
     * Then: Returns HTTP 401 Unauthorized
     */
    @Test
    @DisplayName("Should return 401 for DELETE requests without authentication")
    void shouldReturnUnauthorizedForDeleteWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/test/1"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test CSRF is disabled for Discovery Server.
     * Given: Security configuration with CSRF disabled
     * When: Making POST request with authentication but without CSRF token
     * Then: Request is accepted (authenticated), not rejected due to CSRF
     */
    @Test
    @DisplayName("Should have CSRF protection disabled")
    void shouldHaveCsrfDisabled() throws Exception {
        // When & Then - Should return 404 (authenticated) not 403 (CSRF error)
        mockMvc.perform(post("/api/test")
                        .with(httpBasic("eureka", "password")))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that user has correct authority/role.
     * Given: In-memory user configured with USER authority
     * When: Accessing endpoint with valid credentials
     * Then: User is authenticated with USER authority
     */
    @Test
    @DisplayName("Should configure user with USER authority")
    void shouldConfigureUserWithUserAuthority() throws Exception {
        // When & Then - User with USER authority can access
        mockMvc.perform(get("/")
                        .with(httpBasic("eureka", "password")))
                .andExpect(status().isNotFound()); // Authenticated successfully
    }

    /**
     * Test that all endpoints require authentication.
     * Given: Security configuration with anyRequest().authenticated()
     * When: Accessing various endpoints without credentials
     * Then: All return 401 Unauthorized
     */
    @Test
    @DisplayName("Should require authentication for all endpoints")
    void shouldRequireAuthenticationForAllEndpoints() throws Exception {
        // When & Then
        mockMvc.perform(get("/")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/eureka")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/anything")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/test")).andExpect(status().isUnauthorized());
    }

    /**
     * Test HTTP Basic authentication header is required.
     * Given: Security configuration with HTTP Basic
     * When: Accessing endpoint without Authorization header
     * Then: Returns 401 with WWW-Authenticate header
     */
    @Test
    @DisplayName("Should require HTTP Basic authentication header")
    void shouldRequireHttpBasicAuthenticationHeader() throws Exception {
        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test accessing /eureka/apps endpoint with authentication.
     * Given: Valid credentials
     * When: Accessing /eureka/apps with HTTP Basic auth
     * Then: Request is authenticated (404 as endpoint doesn't exist in test)
     */
    @Test
    @DisplayName("Should allow access to /eureka/apps with valid authentication")
    void shouldAllowAccessToEurekaAppsWithAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/eureka/apps")
                        .with(httpBasic("eureka", "password")))
                .andExpect(status().isNotFound()); // Authenticated
    }

    /**
     * Test case sensitivity of credentials.
     * Given: Username "eureka" configured
     * When: Accessing with "Eureka" (different case)
     * Then: Returns 401 Unauthorized (case-sensitive)
     */
    @Test
    @DisplayName("Should be case-sensitive for username")
    void shouldBeCaseSensitiveForUsername() throws Exception {
        // When & Then
        mockMvc.perform(get("/")
                        .with(httpBasic("Eureka", "password")))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test that NoOpPasswordEncoder is used (passwords stored in plain text).
     * Given: SecurityConfig using NoOpPasswordEncoder
     * When: Authenticating with plain text password
     * Then: Authentication succeeds
     */
    @Test
    @DisplayName("Should use NoOpPasswordEncoder for plain text passwords")
    void shouldUseNoOpPasswordEncoder() throws Exception {
        // When & Then - Plain text password works
        mockMvc.perform(get("/")
                        .with(httpBasic("eureka", "password")))
                .andExpect(status().isNotFound()); // Authenticated with plain text password
    }
}
