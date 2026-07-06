package com.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.event.*;
import com.payment.model.OutboxEvent;
import com.payment.model.OutboxStatus;
import com.payment.model.Payment;
import com.payment.model.PaymentStatus;
import com.payment.repository.OutboxEventRepository;
import com.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        log.info("Procesando pago para pedido: {}", event.getOrderId());

        if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.warn("Pago para pedido {} ya existe. Ignorando evento duplicado.", event.getOrderId());
            return;
        }

        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getTotal())
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        boolean approved = ThreadLocalRandom.current().nextInt(1, 101) <= 80;

        if (approved) {
            savedPayment.setStatus(PaymentStatus.APPROVED);
            savedPayment.setTransactionId(generateTransactionId());
            Payment approvedPayment = paymentRepository.saveAndFlush(savedPayment);
            log.info("Pago aprobado para pedido {}. TransactionId: {}", event.getOrderId(), approvedPayment.getTransactionId());

            PaymentProcessedEvent processedEvent = mapToProcessedEvent(approvedPayment);
            saveOutboxEvent(event.getOrderId(), processedEvent);
        } else {
            savedPayment.setStatus(PaymentStatus.DECLINED);
            Payment declinedPayment = paymentRepository.saveAndFlush(savedPayment);
            log.info("Pago declinado para pedido {}", event.getOrderId());

            PaymentFailedEvent failedEvent = mapToFailedEvent(declinedPayment, "Pago declinado por el banco emisor");
            saveOutboxEvent(event.getOrderId(), failedEvent);
        }
    }

    @Transactional
    public void processRefund(InventoryShortageEvent event) {
        log.info("Procesando reembolso para pedido: {}", event.getOrderId());

        Payment payment = paymentRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("No existe pago para el pedido: " + event.getOrderId()));

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            log.warn("No se puede reembolsar el pago {} porque no está aprobado. Estado actual: {}",
                    payment.getId(), payment.getStatus());
            return;
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment refundedPayment = paymentRepository.saveAndFlush(payment);
        log.info("Pago reembolsado para pedido {}. TransactionId: {}", event.getOrderId(), refundedPayment.getTransactionId());

        PaymentRefundedEvent refundedEvent = mapToRefundedEvent(refundedPayment, event.getReason());
        saveOutboxEvent(event.getOrderId(), refundedEvent);
    }

    private void saveOutboxEvent(UUID orderId, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(orderId)
                    .eventType(event.getClass().getSimpleName())
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.info("Evento {} guardado en outbox para pedido {}", event.getClass().getSimpleName(), orderId);
        } catch (Exception ex) {
            log.error("Error al guardar evento en outbox para pedido {}", orderId, ex);
            throw new RuntimeException("No se pudo guardar el evento en outbox", ex);
        }
    }

    private PaymentProcessedEvent mapToProcessedEvent(Payment payment) {
        return PaymentProcessedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus().name())
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    private PaymentFailedEvent mapToFailedEvent(Payment payment, String reason) {
        return PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .reason(reason)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    private PaymentRefundedEvent mapToRefundedEvent(Payment payment, String reason) {
        return PaymentRefundedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .reason(reason)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
