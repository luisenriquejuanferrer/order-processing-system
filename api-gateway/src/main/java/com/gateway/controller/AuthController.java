package com.gateway.controller;

import com.gateway.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "API Gateway", description = "Autenticación y generación de tokens JWT")
public class AuthController {

    private final JwtUtils jwtUtils;

    private static final List<String> VALID_CREDENTIALS = List.of("demo", "admin");

    @PostMapping("/token")
    @Operation(summary = "Generar token JWT", description = "Autentica al usuario y devuelve un token JWT válido para acceder a los endpoints protegidos")
    public Mono<ResponseEntity<AuthResponse>> getToken(@RequestBody LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        if (!isValidUser(request.getUsername(), request.getPassword())) {
            return Mono.just(ResponseEntity.status(401).build());
        }

        String token = jwtUtils.generateToken(request.getUsername());
        return Mono.just(ResponseEntity.ok(new AuthResponse(token, jwtUtils.getExpirationMs())));
    }

    private boolean isValidUser(String username, String password) {
        return VALID_CREDENTIALS.contains(username) && "demo".equals(password);
    }

    @Value
    public static class LoginRequest {
        String username;
        String password;
    }

    @Value
    public static class AuthResponse {
        String token;
        long expiresIn;
    }
}
