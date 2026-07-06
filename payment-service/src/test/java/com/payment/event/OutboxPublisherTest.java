package com.payment.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.model.OutboxEvent;
import com.payment.model.OutboxStatus;
import com.payment.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    private PaymentEventProducer eventProducer;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(outboxEventRepository, eventProducer, objectMapper);
    }

    @Test
    void publishPendingEvents_withPaymentProcessedEvent_publishesAndMarksPublished() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String payload = "{\"eventId\":\"" + eventId + "\"}";

        OutboxEvent pendingEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(orderId)
                .eventType("PaymentProcessedEvent")
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .userId("user1")
                .amount(BigDecimal.TEN)
                .transactionId("TXN-12345678")
                .status("APPROVED")
                .timestamp(System.currentTimeMillis())
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(pendingEvent));
        when(objectMapper.readValue(payload, PaymentProcessedEvent.class)).thenReturn(event);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        publisher.publishPendingEvents();

        // Then
        verify(eventProducer).publishPaymentProcessed(event);

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
