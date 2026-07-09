package com.notification.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${topics.payments}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = detectEventType(node);

            switch (eventType) {
                case "PaymentFailedEvent" -> {
                    PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
                    log.info("Recibido PaymentFailedEvent para pedido: {}", event.getOrderId());
                    notificationService.handlePaymentFailed(event);
                }
                default -> log.info("Ignorando evento de payments: {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Error al procesar evento de payments: {}", payload, ex);
            throw new RuntimeException("Error al procesar evento de payments", ex);
        }
    }

    @KafkaListener(topics = "${topics.inventory}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = detectEventType(node);

            if ("InventoryShortageEvent".equals(eventType)) {
                InventoryShortageEvent event = objectMapper.readValue(payload, InventoryShortageEvent.class);
                log.info("Recibido InventoryShortageEvent para pedido: {}", event.getOrderId());
                notificationService.handleInventoryShortage(event);
            } else {
                log.info("Ignorando evento de inventory: {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Error al procesar evento de inventory: {}", payload, ex);
            throw new RuntimeException("Error al procesar evento de inventory", ex);
        }
    }

    @KafkaListener(topics = "${topics.orders}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = detectOrderEventType(node);

            if ("OrderConfirmedEvent".equals(eventType)) {
                OrderConfirmedEvent event = objectMapper.readValue(payload, OrderConfirmedEvent.class);
                log.info("Recibido OrderConfirmedEvent para pedido: {}", event.getOrderId());
                notificationService.handleOrderConfirmed(event);
            } else {
                log.info("Ignorando evento de orders: {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Error al procesar evento de orders: {}", payload, ex);
            throw new RuntimeException("Error al procesar evento de orders", ex);
        }
    }

    private String detectOrderEventType(JsonNode node) {
        if (node.has("items")) {
            return "OrderCreatedEvent";
        }
        if (node.has("userId") && !node.has("amount") && !node.has("reason")) {
            return "OrderConfirmedEvent";
        }
        return "Unknown";
    }

    private String detectEventType(JsonNode node) {
        if (node.has("reason") && !node.get("reason").isNull()) {
            if (node.has("transactionId")) {
                return "PaymentRefundedEvent";
            }
            if (node.has("amount")) {
                return "PaymentFailedEvent";
            }
            return "InventoryShortageEvent";
        }
        if (node.has("status") && "APPROVED".equals(node.get("status").asText())) {
            return "PaymentProcessedEvent";
        }
        if (node.has("status") && "RESERVED".equals(node.get("status").asText())) {
            return "InventoryReservedEvent";
        }
        return "Unknown";
    }
}
