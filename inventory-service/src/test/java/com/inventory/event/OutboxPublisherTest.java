package com.inventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.model.OutboxEvent;
import com.inventory.model.OutboxStatus;
import com.inventory.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private InventoryEventProducer eventProducer;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(outboxEventRepository, eventProducer, objectMapper);
    }

    @Test
    void publishPendingEvents_withInventoryReservedEvent_publishesAndMarksPublished() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String payload = "{\"eventId\":\"" + eventId + "\"}";

        OutboxEvent pendingEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(orderId)
                .eventType("InventoryReservedEvent")
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .status("RESERVED")
                .items(List.of())
                .timestamp(System.currentTimeMillis())
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(pendingEvent));
        when(objectMapper.readValue(payload, InventoryReservedEvent.class)).thenReturn(event);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        publisher.publishPendingEvents();

        // Then
        verify(eventProducer).publishInventoryReserved(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    void publishPendingEvents_withInventoryShortageEvent_publishesAndMarksPublished() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String payload = "{\"eventId\":\"" + eventId + "\"}";

        OutboxEvent pendingEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(orderId)
                .eventType("InventoryShortageEvent")
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        InventoryShortageEvent event = InventoryShortageEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .reason("Stock insuficiente")
                .timestamp(System.currentTimeMillis())
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(pendingEvent));
        when(objectMapper.readValue(payload, InventoryShortageEvent.class)).thenReturn(event);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        publisher.publishPendingEvents();

        // Then
        verify(eventProducer).publishInventoryShortage(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    void publishPendingEvents_noPendingEvents_doesNothing() {
        // Given
        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of());

        // When
        publisher.publishPendingEvents();

        // Then
        verifyNoInteractions(eventProducer);
    }
}
