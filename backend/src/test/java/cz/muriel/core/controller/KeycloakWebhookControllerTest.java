package cz.muriel.core.controller;

import cz.muriel.core.dto.KeycloakWebhookEventDto;
import cz.muriel.core.service.KeycloakEventProjectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class) @WebMvcTest(KeycloakWebhookController.class) @TestPropertySource(properties = {
        "app.keycloak.webhook.secret=test-secret" })
class KeycloakWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private KeycloakEventProjectionService projectionService;

    @InjectMocks
    private KeycloakWebhookController controller;

    @Test
    void shouldProcessValidWebhookEvent() throws Exception {
        // Given
        String jsonPayload = """
                {
                    "eventType": "USER_UPDATED",
                    "time": 1234567890000,
                    "realmId": "realm-123",
                    "realmName": "core-platform",
                    "tenantKey": "test-tenant",
                    "userId": "user-123",
                    "username": "testuser",
                    "email": "test@example.com",
                    "firstName": "Test",
                    "lastName": "User",
                    "enabled": true,
                    "attributes": {},
                    "roles": {"realm": [], "clients": {}},
                    "groups": []
                }
                """;

        // When & Then
        mockMvc.perform(post("/internal/keycloak/events").header("X-Webhook-Secret", "test-secret")
                .header("X-Forwarded-For", "172.20.0.1").contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload)).andExpect(status().isOk());

        verify(projectionService, times(1)).processEvent(any(KeycloakWebhookEventDto.class));
    }

    @Test
    void shouldRejectRequestWithInvalidSecret() throws Exception {
        // Given
        String jsonPayload = """
                {
                    "eventType": "USER_UPDATED",
                    "tenantKey": "test-tenant",
                    "userId": "user-123"
                }
                """;

        // When & Then
        mockMvc.perform(post("/internal/keycloak/events").header("X-Webhook-Secret", "wrong-secret")
                .contentType(MediaType.APPLICATION_JSON).content(jsonPayload))
                .andExpect(status().isUnauthorized());

        verify(projectionService, never()).processEvent(any());
    }

    @Test
    void shouldRejectRequestWithoutSecret() throws Exception {
        // Given
        String jsonPayload = """
                {
                    "eventType": "USER_UPDATED",
                    "tenantKey": "test-tenant",
                    "userId": "user-123"
                }
                """;

        // When & Then
        mockMvc.perform(post("/internal/keycloak/events").contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload)).andExpect(status().isUnauthorized());

        verify(projectionService, never()).processEvent(any());
    }

    @Test
    void shouldRejectExternalRequest() throws Exception {
        // Given
        String jsonPayload = """
                {
                    "eventType": "USER_UPDATED",
                    "tenantKey": "test-tenant",
                    "userId": "user-123"
                }
                """;

        // When & Then - simulate external IP
        mockMvc.perform(post("/internal/keycloak/events").header("X-Webhook-Secret", "test-secret")
                .header("X-Forwarded-For", "203.0.113.1") // External IP
                .contentType(MediaType.APPLICATION_JSON).content(jsonPayload))
                .andExpect(status().isForbidden());

        verify(projectionService, never()).processEvent(any());
    }

    @Test
    void shouldHandleProjectionServiceException() throws Exception {
        // Given
        String jsonPayload = """
                {
                    "eventType": "USER_UPDATED",
                    "tenantKey": "test-tenant",
                    "userId": "user-123",
                    "username": "testuser"
                }
                """;

        doThrow(new RuntimeException("Database error")).when(projectionService).processEvent(any());

        // When & Then
        mockMvc.perform(post("/internal/keycloak/events").header("X-Webhook-Secret", "test-secret")
                .header("X-Forwarded-For", "172.20.0.1").contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload)).andExpect(status().isInternalServerError());

        verify(projectionService, times(1)).processEvent(any(KeycloakWebhookEventDto.class));
    }
}
