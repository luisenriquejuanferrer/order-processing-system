package com.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.event.OrderCreatedEvent;
import com.payment.event.OrderItemEvent;
import com.payment.event.PaymentFailedEvent;
import com.payment.event.PaymentProcessedEvent;
import com.payment.model.OutboxEvent;
import com.payment.model.OutboxStatus;
import com.payment.model.Payment;
import com.payment.model.PaymentStatus;
import com.payment.repository.OutboxEventRepository;
import com.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    private OrderCreatedEvent orderCreatedEvent;

    @BeforeEach
    void setUp() {
        orderCreatedEvent = OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId("user1")
                .total(new BigDecimal("25.00"))
                .items(List.of(OrderItemEvent.builder()
                        .productId("p1")
                        .quantity(1)
                        .unitPrice(new BigDecimal("25.00"))
                        .build()))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Test
    void processPayment_success_savesPaymentAndOutboxEvent() throws Exception {
        // Given
        Payment savedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderCreatedEvent.getOrderId())
                .userId("user1")
                .amount(new BigDecimal("25.00"))
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderCreatedEvent.getOrderId())).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);
        when(objectMapper.writeValueAsString(any(PaymentProcessedEvent.class))).thenReturn("{}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        paymentService.processPayment(orderCreatedEvent);

        // Then
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).saveAndFlush(paymentCaptor.capture());

        List<Payment> payments = paymentCaptor.getAllValues();
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payments.get(1).getStatus()).isIn(PaymentStatus.APPROVED, PaymentStatus.DECLINED);

        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void processPayment_duplicateEvent_ignored() {
        // Given
        Payment existingPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderCreatedEvent.getOrderId())
                .build();

        when(paymentRepository.findByOrderId(orderCreatedEvent.getOrderId())).thenReturn(Optional.of(existingPayment));

        // When
        paymentService.processPayment(orderCreatedEvent);

        // Then
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void processRefund_success_updatesStatusAndSavesOutboxEvent() throws Exception {
        // Given
        UUID orderId = UUID.randomUUID();
        com.payment.event.InventoryShortageEvent shortageEvent = com.payment.event.InventoryShortageEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .reason("Stock insuficiente")
                .timestamp(System.currentTimeMillis())
                .build();

        Payment approvedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .amount(new BigDecimal("25.00"))
                .status(PaymentStatus.APPROVED)
                .transactionId("TXN-12345678")
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(approvedPayment));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(approvedPayment);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        paymentService.processRefund(shortageEvent);

        // Then
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void processRefund_paymentNotFound_throwsException() {
        // Given
        UUID orderId = UUID.randomUUID();
        com.payment.event.InventoryShortageEvent shortageEvent = com.payment.event.InventoryShortageEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .reason("Stock insuficiente")
                .timestamp(System.currentTimeMillis())
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> paymentService.processRefund(shortageEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe pago para el pedido");
    }
}
