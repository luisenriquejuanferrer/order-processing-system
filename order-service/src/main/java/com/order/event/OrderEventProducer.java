package com.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Value("${topics.orders}")
    private String ordersTopic;

    void setOrdersTopic(String ordersTopic) {
        this.ordersTopic = ordersTopic;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publicando evento OrderCreated: {} en topic: {}", event.getEventId(), ordersTopic);
        kafkaTemplate.send(ordersTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error al publicar evento OrderCreated {}", event.getEventId(), ex);
                    } else {
                        log.info("Evento OrderCreated {} publicado correctamente en partition {}, offset {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
