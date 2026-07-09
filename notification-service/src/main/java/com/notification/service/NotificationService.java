package com.notification.service;

import com.notification.event.InventoryShortageEvent;
import com.notification.event.OrderConfirmedEvent;
import com.notification.event.PaymentFailedEvent;
import com.notification.model.Notification;
import com.notification.model.NotificationType;
import com.notification.model.ProcessedEvent;
import com.notification.repository.NotificationRepository;
import com.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByOrderId(UUID orderId) {
        return notificationRepository.findByOrderIdOrderBySentAtDesc(orderId);
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        if (isEventAlreadyProcessed(event.getEventId().toString())) {
            log.warn("Evento PaymentFailedEvent {} ya fue procesado. Ignorando.", event.getEventId());
            return;
        }

        String message = String.format("El pago de %.2f EUR para el pedido %s ha sido rechazado. Motivo: %s",
                event.getAmount(), event.getOrderId(), event.getReason());

        saveNotification(event.getOrderId(), event.getUserId(), NotificationType.PAYMENT_FAILED, message);
        markEventAsProcessed(event.getEventId().toString(), "PaymentFailedEvent");

        log.info("Notificación PAYMENT_FAILED creada para pedido {}", event.getOrderId());
    }

    @Transactional
    public void handleInventoryShortage(InventoryShortageEvent event) {
        if (isEventAlreadyProcessed(event.getEventId().toString())) {
            log.warn("Evento InventoryShortageEvent {} ya fue procesado. Ignorando.", event.getEventId());
            return;
        }

        String message = String.format("No se pudo completar el pedido %s por falta de stock: %s",
                event.getOrderId(), event.getReason());

        saveNotification(event.getOrderId(), null, NotificationType.INVENTORY_SHORTAGE, message);
        markEventAsProcessed(event.getEventId().toString(), "InventoryShortageEvent");

        log.info("Notificación INVENTORY_SHORTAGE creada para pedido {}", event.getOrderId());
    }

    @Transactional
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        if (isEventAlreadyProcessed(event.getEventId().toString())) {
            log.warn("Evento OrderConfirmedEvent {} ya fue procesado. Ignorando.", event.getEventId());
            return;
        }

        String message = String.format("El pedido %s ha sido confirmado correctamente.", event.getOrderId());

        saveNotification(event.getOrderId(), event.getUserId(), NotificationType.ORDER_CONFIRMED, message);
        markEventAsProcessed(event.getEventId().toString(), "OrderConfirmedEvent");

        log.info("Notificación ORDER_CONFIRMED creada para pedido {}", event.getOrderId());
    }

    private void saveNotification(UUID orderId, String userId, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .orderId(orderId)
                .userId(userId != null ? userId : "unknown")
                .type(type)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }

    private boolean isEventAlreadyProcessed(String eventId) {
        return processedEventRepository.findByEventId(eventId).isPresent();
    }

    private void markEventAsProcessed(String eventId, String eventType) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .build();
        processedEventRepository.save(processedEvent);
    }
}
