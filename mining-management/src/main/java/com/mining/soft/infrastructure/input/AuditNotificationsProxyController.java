package com.mining.soft.infrastructure.input;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuditNotificationsProxyController {

    private final RestClient restClient;

    public AuditNotificationsProxyController(
            RestClient.Builder restClientBuilder,
            @Value("${mining.audit-notifications.url:http://:8082}") String auditNotificationsServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(auditNotificationsServiceUrl).build();
    }

    private RestClient.RequestHeadersSpec<?> withAuthorization(RestClient.RequestHeadersSpec<?> request,
                                                               String authorization) {
        return authorization != null && !authorization.isBlank()
                ? request.header(HttpHeaders.AUTHORIZATION, authorization)
                : request;
    }

    @GetMapping("/notifications/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> getNotifications(@PathVariable Long userId,
                                                   @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return withAuthorization(restClient.get()
                .uri("/api/v1/notifications/{userId}", userId)
                , authorization)
                .retrieve()
                .toEntity(Object.class);
    }

    @GetMapping("/notifications/{userId}/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Object> getUnreadNotifications(@PathVariable Long userId,
                                                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return withAuthorization(restClient.get()
                .uri("/api/v1/notifications/{userId}/unread", userId)
                , authorization)
                .retrieve()
                .toEntity(Object.class);
    }

    @PatchMapping("/notifications/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable String id,
                                           @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return withAuthorization(restClient.patch()
                .uri("/api/v1/notifications/{id}/read", id)
                , authorization)
                .retrieve()
                .toBodilessEntity();
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Object> getAuditLogs(@RequestParam Map<String, String> params,
                                               @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return withAuthorization(restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/audit");
                    params.forEach(builder::queryParam);
                    return builder.build();
                })
                , authorization)
                .retrieve()
                .toEntity(Object.class);
    }
}
