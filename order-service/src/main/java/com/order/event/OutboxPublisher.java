package com.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.model.OutboxEvent;
import com.order.model.OutboxStatus;
import com.order.repository.OutboxEventRepository;
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
    private final OrderEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    /**
     * Lee eventos pendientes de la tabla outbox y los publica en Kafka.
     * Se ejecuta cada 5 segundos.
     *
     * Nota: si la publicación en Kafka tiene éxito pero falla el marcado como PUBLICADO,
     * el evento se reintentará en la siguiente ejecución. Esto puede generar duplicados,
     * por eso los consumidores deben ser idempotentes (tabla processed_events).
     */
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
                    case "OrderCreatedEvent" -> {
                        OrderCreatedEvent event = objectMapper.readValue(outboxEvent.getPayload(), OrderCreatedEvent.class);
                        eventProducer.publishOrderCreated(event);
                    }
                    case "OrderConfirmedEvent" -> {
                        OrderConfirmedEvent event = objectMapper.readValue(outboxEvent.getPayload(), OrderConfirmedEvent.class);
                        eventProducer.publishOrderConfirmed(event);
                    }
                    default -> throw new IllegalArgumentException("Tipo de evento desconocido: " + outboxEvent.getEventType());
                }

                outboxEvent.setStatus(OutboxStatus.PUBLISHED);
                outboxEvent.setPublishedAt(OffsetDateTime.now());
                outboxEventRepository.save(outboxEvent);

                log.info("Evento outbox {} publicado correctamente", outboxEvent.getId());
            } catch (Exception ex) {
                log.error("Error al publicar evento outbox {}. Se reintentará.", outboxEvent.getId(), ex);
                // No lanzamos la excepción para que el resto de eventos puedan procesarse
            }
        }
    }
}
