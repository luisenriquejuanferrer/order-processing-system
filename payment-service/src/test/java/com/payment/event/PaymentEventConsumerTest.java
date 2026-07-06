package com.payment.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentEventConsumer consumer;

    @Test
    void handleOrderCreated_deserializesAndProcesses() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\"}";
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId("user1")
                .total(BigDecimal.TEN)
                .items(List.of())
                .timestamp(System.currentTimeMillis())
                .build();

        when(objectMapper.readValue(payload, OrderCreatedEvent.class)).thenReturn(event);

        // When
        consumer.handleOrderCreated(payload);

        // Then
        verify(paymentService).processPayment(event);
    }

    @Test
    void handleInventoryShortage_deserializesAndProcesses() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\"}";
        InventoryShortageEvent event = InventoryShortageEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .reason("Stock insuficiente")
                .timestamp(System.currentTimeMillis())
                .build();

        when(objectMapper.readValue(payload, InventoryShortageEvent.class)).thenReturn(event);

        // When
        consumer.handleInventoryShortage(payload);

        // Then
        verify(paymentService).processRefund(event);
    }
}
