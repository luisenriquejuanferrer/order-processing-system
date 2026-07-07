package com.payment.event;

import com.fasterxml.jackson.databind.JsonNode;
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
    void handleInventoryEvent_withReason_deserializesAndProcesses() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"reason\":\"Stock insuficiente\"}";
        InventoryShortageEvent event = InventoryShortageEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .reason("Stock insuficiente")
                .timestamp(System.currentTimeMillis())
                .build();

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode reasonNode = mock(JsonNode.class);
        
        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("reason")).thenReturn(true);
        when(jsonNode.get("reason")).thenReturn(reasonNode);
        when(reasonNode.isNull()).thenReturn(false);
        when(objectMapper.readValue(payload, InventoryShortageEvent.class)).thenReturn(event);

        // When
        consumer.handleInventoryEvent(payload);

        // Then
        verify(paymentService).processRefund(event);
    }

    @Test
    void handleInventoryEvent_withoutReason_ignoresEvent() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"RESERVED\"}";

        JsonNode jsonNode = mock(JsonNode.class);
        
        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("reason")).thenReturn(false);

        // When
        consumer.handleInventoryEvent(payload);

        // Then
        verifyNoInteractions(paymentService);
    }
}
