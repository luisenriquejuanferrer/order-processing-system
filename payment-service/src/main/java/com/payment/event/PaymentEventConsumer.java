package com.payment.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.model.ProcessedEvent;
import com.payment.repository.ProcessedEventRepository;
import com.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "${topics.orders}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleOrderCreated(String payload) throws Exception {
        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        processIfNotDuplicate(event.getEventId().toString(), "OrderCreatedEvent", () -> {
            log.info("Recibido OrderCreatedEvent para pedido: {}", event.getOrderId());
            paymentService.processPayment(event);
        });
    }

    @KafkaListener(topics = "${topics.inventory}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleInventoryEvent(String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);

        if (node.has("reason") && !node.get("reason").isNull()) {
            InventoryShortageEvent event = objectMapper.readValue(payload, InventoryShortageEvent.class);
            processIfNotDuplicate(event.getEventId().toString(), "InventoryShortageEvent", () -> {
                log.info("Recibido InventoryShortageEvent para pedido: {}", event.getOrderId());
                paymentService.processRefund(event);
            });
        } else {
            log.info("Ignorando evento de inventory (no es shortage)");
        }
    }

    private void processIfNotDuplicate(String eventId, String eventType, Runnable processor) {
        if (processedEventRepository.findByEventId(eventId).isPresent()) {
            log.warn("Evento {} con id {} ya fue procesado. Ignorando.", eventType, eventId);
            return;
        }

        processor.run();

        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .build();
        processedEventRepository.save(processedEvent);
    }
}
