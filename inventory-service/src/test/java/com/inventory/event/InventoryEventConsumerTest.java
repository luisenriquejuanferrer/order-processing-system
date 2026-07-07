package com.inventory.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InventoryEventConsumer consumer;

    @Test
    void handlePaymentProcessed_withApprovedStatusAndItems_processesEvent() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"APPROVED\",\"items\":[{\"productId\":\"p1\",\"quantity\":2,\"unitPrice\":10.00}]}";
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId("user1")
                .status("APPROVED")
                .items(List.of(OrderItemEvent.builder()
                        .productId("p1")
                        .quantity(2)
                        .unitPrice(new java.math.BigDecimal("10.00"))
                        .build()))
                .timestamp(System.currentTimeMillis())
                .build();

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode statusNode = mock(JsonNode.class);
        JsonNode itemsNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("status")).thenReturn(true);
        when(jsonNode.get("status")).thenReturn(statusNode);
        when(statusNode.asText()).thenReturn("APPROVED");
        when(jsonNode.has("items")).thenReturn(true);
        when(jsonNode.get("items")).thenReturn(itemsNode);
        when(itemsNode.isNull()).thenReturn(false);
        when(itemsNode.isArray()).thenReturn(true);
        when(objectMapper.readValue(payload, PaymentProcessedEvent.class)).thenReturn(event);

        // When
        consumer.handlePaymentProcessed(payload);

        // Then
        verify(inventoryService).reserveStock(event);
    }

    @Test
    void handlePaymentProcessed_withNonApprovedStatus_ignoresEvent() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"DECLINED\"}";

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode statusNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("status")).thenReturn(true);
        when(jsonNode.get("status")).thenReturn(statusNode);
        when(statusNode.asText()).thenReturn("DECLINED");

        // When
        consumer.handlePaymentProcessed(payload);

        // Then
        verifyNoInteractions(inventoryService);
    }

    @Test
    void handlePaymentProcessed_withoutItems_ignoresEvent() throws Exception {
        // Given
        String payload = "{\"orderId\":\"123e4567-e89b-12d3-a456-426614174000\",\"status\":\"APPROVED\"}";

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode statusNode = mock(JsonNode.class);

        when(objectMapper.readTree(payload)).thenReturn(jsonNode);
        when(jsonNode.has("status")).thenReturn(true);
        when(jsonNode.get("status")).thenReturn(statusNode);
        when(statusNode.asText()).thenReturn("APPROVED");
        when(jsonNode.has("items")).thenReturn(false);

        // When
        consumer.handlePaymentProcessed(payload);

        // Then
        verifyNoInteractions(inventoryService);
    }
}
