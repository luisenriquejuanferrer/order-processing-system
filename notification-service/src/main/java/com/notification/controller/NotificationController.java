package com.notification.controller;

import com.notification.dto.NotificationResponse;
import com.notification.model.Notification;
import com.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Service", description = "API para gestionar notificaciones")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Listar todas las notificaciones", description = "Obtiene la lista completa de notificaciones del sistema")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        List<NotificationResponse> notifications = notificationService.getAllNotifications().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Obtener notificaciones por Order ID", description = "Busca todas las notificaciones asociadas a un pedido específico")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByOrderId(@PathVariable UUID orderId) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .orderId(notification.getOrderId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .message(notification.getMessage())
                .sentAt(notification.getSentAt())
                .build();
    }
}
