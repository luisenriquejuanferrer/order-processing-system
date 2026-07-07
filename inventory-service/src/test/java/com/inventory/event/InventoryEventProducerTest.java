package com.inventory.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private InventoryEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new InventoryEventProducer(kafkaTemplate);
        producer.setInventoryTopic("inventory");
    }

    @Test
    void publishInventoryReserved_sendsToCorrectTopic() {
        // Given
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .status("RESERVED")
                .items(List.of(ReservedItemEvent.builder()
                        .productId("p1")
                        .quantity(2)
                        .unitPrice(new java.math.BigDecimal("10.00"))
                        .build()))
                .timestamp(System.currentTimeMillis())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        // When
        producer.publishInventoryReserved(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("inventory");
        assertThat(keyCaptor.getValue()).isEqualTo(orderId.toString());
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    void publishInventoryShortage_sendsToCorrectTopic() {
        // Given
        UUID orderId = UUID.randomUUID();
        InventoryShortageEvent event = InventoryShortageEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .reason("Stock insuficiente")
                .timestamp(System.currentTimeMillis())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        // When
        producer.publishInventoryShortage(event);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }
}
