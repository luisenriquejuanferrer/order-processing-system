package com.payment.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${topics.orders}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(String payload) throws Exception {
        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        log.info("Recibido OrderCreatedEvent para pedido: {}", event.getOrderId());
        paymentService.processPayment(event);
    }

    @KafkaListener(topics = "${topics.inventory}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryEvent(String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);

        if (node.has("reason") && !node.get("reason").isNull()) {
            InventoryShortageEvent event = objectMapper.readValue(payload, InventoryShortageEvent.class);
            log.info("Recibido InventoryShortageEvent para pedido: {}", event.getOrderId());
            paymentService.processRefund(event);
        } else {
            log.info("Ignorando evento de inventory (no es shortage)");
        }
    }
}
