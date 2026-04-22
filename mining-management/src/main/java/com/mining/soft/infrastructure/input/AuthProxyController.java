package com.mining.soft.infrastructure.input;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthProxyController {

    private final RestClient restClient;

    public AuthProxyController(RestClient.Builder restClientBuilder,
                               @Value("${mining.users.url:http://localhost:8081}") String authServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(authServiceUrl).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, Object> loginRequest) {
        ResponseEntity<Object> response = restClient.post()
                .uri("/auth/login")
                .body(loginRequest)
                .retrieve()
                .toEntity(Object.class);

        return ResponseEntity
                .status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }
}
