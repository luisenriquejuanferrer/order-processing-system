package com.payment.event;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private PaymentEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new PaymentEventProducer(kafkaTemplate);
        producer.setPaymentsTopic("payments");
    }

    @Test
    void publishPaymentProcessed_sendsToCorrectTopic() {
        // Given
        UUID orderId = UUID.randomUUID();
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .amount(BigDecimal.TEN)
                .transactionId("TXN-12345678")
                .status("APPROVED")
                .timestamp(System.currentTimeMillis())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        // When
        producer.publishPaymentProcessed(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("payments");
        assertThat(keyCaptor.getValue()).isEqualTo(orderId.toString());
        assertThat(eventCaptor.getValue()).isEqualTo(event);
    }

    @Test
    void publishPaymentFailed_sendsToCorrectTopic() {
        // Given
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .amount(BigDecimal.TEN)
                .reason("Tarjeta rechazada")
                .timestamp(System.currentTimeMillis())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(new CompletableFuture<>());

        // When
        producer.publishPaymentFailed(event);

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }
}
