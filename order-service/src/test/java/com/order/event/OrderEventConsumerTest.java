package com.order.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.repository.ProcessedEventRepository;
import com.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    void handlePaymentEvent_paymentProcessed_confirmsOrder() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"APPROVED\"}";
        UUID orderId = UUID.randomUUID();
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .amount(BigDecimal.TEN)
                .transactionId("TXN-12345678")
                .status("APPROVED")
                .items(List.of())
                .timestamp(System.currentTimeMillis())
                .build();

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode statusNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("status")).thenReturn(true);
        when(jsonNode.get("status")).thenReturn(statusNode);
        when(statusNode.asText()).thenReturn("APPROVED");
        when(objectMapper.readValue(payload, PaymentProcessedEvent.class)).thenReturn(event);
        when(processedEventRepository.findByEventId(event.getEventId().toString())).thenReturn(Optional.empty());

        // When
        consumer.handlePaymentEvent(payload);

        // Then
        verify(orderService).markAsConfirmed(orderId);
    }

    @Test
    void handlePaymentEvent_paymentFailed_failsOrder() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"reason\":\"Pago rechazado\"}";
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .amount(BigDecimal.TEN)
                .reason("Pago rechazado")
                .timestamp(System.currentTimeMillis())
                .build();

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode reasonNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("status")).thenReturn(false);
        when(jsonNode.has("reason")).thenReturn(true);
        when(jsonNode.get("reason")).thenReturn(reasonNode);
        when(reasonNode.isNull()).thenReturn(false);
        when(objectMapper.readValue(payload, PaymentFailedEvent.class)).thenReturn(event);
        when(processedEventRepository.findByEventId(event.getEventId().toString())).thenReturn(Optional.empty());

        // When
        consumer.handlePaymentEvent(payload);

        // Then
        verify(orderService).markAsFailed(orderId, "Pago rechazado");
    }

    @Test
    void handleInventoryEvent_inventoryReserved_confirmsOrder() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"RESERVED\"}";
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .status("RESERVED")
                .items(List.of())
                .timestamp(System.currentTimeMillis())
                .build();

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode statusNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("status")).thenReturn(true);
        when(jsonNode.get("status")).thenReturn(statusNode);
        when(statusNode.asText()).thenReturn("RESERVED");
        when(objectMapper.readValue(payload, InventoryReservedEvent.class)).thenReturn(event);
        when(processedEventRepository.findByEventId(event.getEventId().toString())).thenReturn(Optional.empty());

        // When
        consumer.handleInventoryEvent(payload);

        // Then
        verify(orderService).markAsConfirmed(orderId);
    }

    @Test
    void handleInventoryEvent_inventoryShortage_cancelsOrder() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"reason\":\"Stock insuficiente\"}";
        UUID orderId = UUID.randomUUID();
        InventoryShortageEvent event = InventoryShortageEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .reason("Stock insuficiente")
                .timestamp(System.currentTimeMillis())
                .build();

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode reasonNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("status")).thenReturn(false);
        when(jsonNode.has("reason")).thenReturn(true);
        when(jsonNode.get("reason")).thenReturn(reasonNode);
        when(reasonNode.isNull()).thenReturn(false);
        when(objectMapper.readValue(payload, InventoryShortageEvent.class)).thenReturn(event);
        when(processedEventRepository.findByEventId(event.getEventId().toString())).thenReturn(Optional.empty());

        // When
        consumer.handleInventoryEvent(payload);

        // Then
        verify(orderService).markAsCancelled(orderId, "Stock insuficiente");
    }

    @Test
    void handlePaymentEvent_duplicateEvent_ignored() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"APPROVED\"}";
        UUID orderId = UUID.randomUUID();
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .status("APPROVED")
                .timestamp(System.currentTimeMillis())
                .build();

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode statusNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("status")).thenReturn(true);
        when(jsonNode.get("status")).thenReturn(statusNode);
        when(statusNode.asText()).thenReturn("APPROVED");
        when(objectMapper.readValue(payload, PaymentProcessedEvent.class)).thenReturn(event);
        when(processedEventRepository.findByEventId(event.getEventId().toString())).thenReturn(Optional.of(mock()));

        // When
        consumer.handlePaymentEvent(payload);

        // Then
        verifyNoInteractions(orderService);
    }
}
