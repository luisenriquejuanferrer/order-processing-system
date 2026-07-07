package com.inventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.model.OutboxEvent;
import com.inventory.model.OutboxStatus;
import com.inventory.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final InventoryEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Publicando {} eventos pendientes del outbox", pendingEvents.size());

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                switch (outboxEvent.getEventType()) {
                    case "InventoryReservedEvent" -> {
                        InventoryReservedEvent event = objectMapper.readValue(outboxEvent.getPayload(), InventoryReservedEvent.class);
                        eventProducer.publishInventoryReserved(event);
                    }
                    case "InventoryShortageEvent" -> {
                        InventoryShortageEvent event = objectMapper.readValue(outboxEvent.getPayload(), InventoryShortageEvent.class);
                        eventProducer.publishInventoryShortage(event);
                    }
                    default -> throw new IllegalArgumentException("Tipo de evento desconocido: " + outboxEvent.getEventType());
                }

                outboxEvent.setStatus(OutboxStatus.PUBLISHED);
                outboxEvent.setPublishedAt(OffsetDateTime.now());
                outboxEventRepository.save(outboxEvent);

                log.info("Evento outbox {} publicado correctamente", outboxEvent.getId());
            } catch (Exception ex) {
                log.error("Error al publicar evento outbox {}. Se reintentará.", outboxEvent.getId(), ex);
            }
        }
    }
}
