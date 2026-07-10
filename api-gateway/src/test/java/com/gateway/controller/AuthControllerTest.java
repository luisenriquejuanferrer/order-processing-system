package com.gateway.controller;

import com.gateway.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtUtils jwtUtils;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(jwtUtils);
        webTestClient = WebTestClient.bindToController(authController).build();
    }

    @Test
    void getToken_withValidCredentials_shouldReturnToken() {
        when(jwtUtils.generateToken("demo")).thenReturn("fake-jwt-token");
        when(jwtUtils.getExpirationMs()).thenReturn(3600000L);

        webTestClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new AuthController.LoginRequest("demo", "demo")), AuthController.LoginRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("fake-jwt-token")
                .jsonPath("$.expiresIn").isEqualTo(3600000L);
    }

    @Test
    void getToken_withInvalidCredentials_shouldReturnUnauthorized() {
        webTestClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new AuthController.LoginRequest("demo", "wrong-password")), AuthController.LoginRequest.class)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getToken_withNullBody_shouldReturnBadRequest() {
        webTestClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.empty(), AuthController.LoginRequest.class)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getToken_withNullUsername_shouldReturnBadRequest() {
        webTestClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new AuthController.LoginRequest(null, "demo")), AuthController.LoginRequest.class)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getToken_withAdminCredentials_shouldReturnToken() {
        when(jwtUtils.generateToken("admin")).thenReturn("admin-jwt-token");
        when(jwtUtils.getExpirationMs()).thenReturn(3600000L);

        webTestClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new AuthController.LoginRequest("admin", "demo")), AuthController.LoginRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("admin-jwt-token");
    }
}
