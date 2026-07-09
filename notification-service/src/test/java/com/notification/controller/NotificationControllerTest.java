package com.notification.controller;

import com.notification.model.Notification;
import com.notification.model.NotificationType;
import com.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void getAllNotifications_returnsList() throws Exception {
        // Given
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId("user1")
                .type(NotificationType.ORDER_CONFIRMED)
                .message("Pedido confirmado")
                .sentAt(OffsetDateTime.now())
                .build();

        when(notificationService.getAllNotifications()).thenReturn(List.of(notification));

        // When & Then
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("ORDER_CONFIRMED"))
                .andExpect(jsonPath("$[0].message").value("Pedido confirmado"));
    }

    @Test
    void getNotificationsByOrderId_returnsList() throws Exception {
        // Given
        UUID orderId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .type(NotificationType.PAYMENT_FAILED)
                .message("Pago rechazado")
                .sentAt(OffsetDateTime.now())
                .build();

        when(notificationService.getNotificationsByOrderId(orderId)).thenReturn(List.of(notification));

        // When & Then
        mockMvc.perform(get("/api/notifications/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$[0].type").value("PAYMENT_FAILED"));
    }
}
