package com.inventory.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.inventory}")
    private String inventoryTopic;

    void setInventoryTopic(String inventoryTopic) {
        this.inventoryTopic = inventoryTopic;
    }

    public void publishInventoryReserved(InventoryReservedEvent event) {
        log.info("Publicando evento InventoryReserved: {} en topic: {}", event.getEventId(), inventoryTopic);
        kafkaTemplate.send(inventoryTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error al publicar InventoryReserved {}", event.getEventId(), ex);
                    } else {
                        log.info("InventoryReserved {} publicado en partition {}, offset {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishInventoryShortage(InventoryShortageEvent event) {
        log.info("Publicando evento InventoryShortage: {} en topic: {}", event.getEventId(), inventoryTopic);
        kafkaTemplate.send(inventoryTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error al publicar InventoryShortage {}", event.getEventId(), ex);
                    } else {
                        log.info("InventoryShortage {} publicado en partition {}, offset {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
