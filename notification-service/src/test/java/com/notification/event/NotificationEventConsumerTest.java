package com.notification.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    void handlePaymentEvent_paymentFailed_processesEvent() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"reason\":\"Pago rechazado\",\"amount\":10.00}";
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId("user1")
                .amount(BigDecimal.TEN)
                .reason("Pago rechazado")
                .timestamp(System.currentTimeMillis())
                .build();

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode reasonNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        lenient().when(jsonNode.has("reason")).thenReturn(true);
        lenient().when(jsonNode.get("reason")).thenReturn(reasonNode);
        lenient().when(reasonNode.isNull()).thenReturn(false);
        lenient().when(jsonNode.has("transactionId")).thenReturn(false);
        lenient().when(jsonNode.has("amount")).thenReturn(true);
        when(objectMapper.readValue(payload, PaymentFailedEvent.class)).thenReturn(event);

        // When
        consumer.handlePaymentEvent(payload);

        // Then
        verify(notificationService).handlePaymentFailed(event);
    }

    @Test
    void handlePaymentEvent_paymentProcessed_ignored() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"APPROVED\"}";

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode statusNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        lenient().when(jsonNode.has("reason")).thenReturn(false);
        lenient().when(jsonNode.has("status")).thenReturn(true);
        lenient().when(jsonNode.get("status")).thenReturn(statusNode);
        lenient().when(statusNode.asText()).thenReturn("APPROVED");

        // When
        consumer.handlePaymentEvent(payload);

        // Then
        verifyNoInteractions(notificationService);
    }

    @Test
    void handleInventoryEvent_inventoryShortage_processesEvent() throws Exception {
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
        lenient().when(jsonNode.has("status")).thenReturn(false);
        lenient().when(jsonNode.has("reason")).thenReturn(true);
        lenient().when(jsonNode.get("reason")).thenReturn(reasonNode);
        lenient().when(reasonNode.isNull()).thenReturn(false);
        lenient().when(jsonNode.has("transactionId")).thenReturn(false);
        lenient().when(jsonNode.has("amount")).thenReturn(false);
        when(objectMapper.readValue(payload, InventoryShortageEvent.class)).thenReturn(event);

        // When
        consumer.handleInventoryEvent(payload);

        // Then
        verify(notificationService).handleInventoryShortage(event);
    }

    @Test
    void handleOrderEvent_orderConfirmed_processesEvent() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"userId\":\"user1\"}";
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId("user1")
                .timestamp(System.currentTimeMillis())
                .build();

        JsonNode jsonNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        lenient().when(jsonNode.has("items")).thenReturn(false);
        lenient().when(jsonNode.has("userId")).thenReturn(true);
        lenient().when(jsonNode.has("amount")).thenReturn(false);
        lenient().when(jsonNode.has("reason")).thenReturn(false);
        when(objectMapper.readValue(payload, OrderConfirmedEvent.class)).thenReturn(event);

        // When
        consumer.handleOrderEvent(payload);

        // Then
        verify(notificationService).handleOrderConfirmed(event);
    }
}
