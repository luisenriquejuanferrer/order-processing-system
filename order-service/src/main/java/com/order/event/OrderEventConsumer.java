package com.order.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.model.ProcessedEvent;
import com.order.repository.ProcessedEventRepository;
import com.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "${topics.payments}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handlePaymentEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = detectPaymentEventType(node);

            switch (eventType) {
                case "PaymentProcessedEvent" -> {
                    PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);
                    processIfNotDuplicate(event.getEventId().toString(), eventType, () -> {
                        log.info("Recibido PaymentProcessedEvent para pedido: {}", event.getOrderId());
                        orderService.markAsConfirmed(event.getOrderId());
                    });
                }
                case "PaymentFailedEvent" -> {
                    PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
                    processIfNotDuplicate(event.getEventId().toString(), eventType, () -> {
                        log.info("Recibido PaymentFailedEvent para pedido: {}", event.getOrderId());
                        orderService.markAsFailed(event.getOrderId(), event.getReason());
                    });
                }
                default -> log.info("Ignorando evento de payments: {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Error al procesar evento de payments: {}", payload, ex);
            throw new RuntimeException("Error al procesar evento de payments", ex);
        }
    }

    @KafkaListener(topics = "${topics.inventory}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleInventoryEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = detectInventoryEventType(node);

            switch (eventType) {
                case "InventoryReservedEvent" -> {
                    InventoryReservedEvent event = objectMapper.readValue(payload, InventoryReservedEvent.class);
                    processIfNotDuplicate(event.getEventId().toString(), eventType, () -> {
                        log.info("Recibido InventoryReservedEvent para pedido: {}", event.getOrderId());
                        orderService.markAsConfirmed(event.getOrderId());
                    });
                }
                case "InventoryShortageEvent" -> {
                    InventoryShortageEvent event = objectMapper.readValue(payload, InventoryShortageEvent.class);
                    processIfNotDuplicate(event.getEventId().toString(), eventType, () -> {
                        log.info("Recibido InventoryShortageEvent para pedido: {}", event.getOrderId());
                        orderService.markAsCancelled(event.getOrderId(), event.getReason());
                    });
                }
                default -> log.info("Ignorando evento de inventory: {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Error al procesar evento de inventory: {}", payload, ex);
            throw new RuntimeException("Error al procesar evento de inventory", ex);
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

    private String detectPaymentEventType(JsonNode node) {
        if (node.has("status") && "APPROVED".equals(node.get("status").asText())) {
            return "PaymentProcessedEvent";
        }
        if (node.has("reason") && !node.get("reason").isNull()) {
            return "PaymentFailedEvent";
        }
        return "Unknown";
    }

    private String detectInventoryEventType(JsonNode node) {
        if (node.has("status") && "RESERVED".equals(node.get("status").asText())) {
            return "InventoryReservedEvent";
        }
        if (node.has("reason") && !node.get("reason").isNull()) {
            return "InventoryShortageEvent";
        }
        return "Unknown";
    }
}
