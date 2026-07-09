package com.order.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new OrderEventProducer(kafkaTemplate);
        producer.setOrdersTopic("orders");
    }

    @Test
    void publishOrderCreated_sendsToCorrectTopicWithOrderIdKey() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .total(BigDecimal.TEN)
                .items(List.of(OrderItemEvent.builder()
                        .productId("p1")
                        .quantity(1)
                        .unitPrice(BigDecimal.TEN)
                        .build()))
                .timestamp(System.currentTimeMillis())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        // When
        producer.publishOrderCreated(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("orders");
        assertThat(keyCaptor.getValue()).isEqualTo(orderId.toString());
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    void publishOrderConfirmed_sendsToCorrectTopicWithOrderIdKey() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .timestamp(System.currentTimeMillis())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        // When
        producer.publishOrderConfirmed(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("orders");
        assertThat(keyCaptor.getValue()).isEqualTo(orderId.toString());
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }
}
