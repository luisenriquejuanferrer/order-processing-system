package com.inventory.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${topics.payments}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentProcessed(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String status = node.has("status") ? node.get("status").asText() : null;

            if (!"APPROVED".equals(status)) {
                log.info("Ignorando evento de payment con status: {}", status);
                return;
            }

            if (!node.has("items") || node.get("items").isNull() || !node.get("items").isArray()) {
                log.warn("Evento PaymentProcessedEvent no tiene items. Ignorando.");
                return;
            }

            PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);
            log.info("Recibido PaymentProcessedEvent para pedido: {}", event.getOrderId());
            inventoryService.reserveStock(event);
        } catch (Exception ex) {
            log.error("Error al procesar evento de payment: {}", payload, ex);
            throw new RuntimeException("Error al procesar evento de payment", ex);
        }
    }
}
