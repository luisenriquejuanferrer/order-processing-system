package com.payment.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${topics.payments}")
    private String paymentsTopic;

    void setPaymentsTopic(String paymentsTopic) {
        this.paymentsTopic = paymentsTopic;
    }

    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Publicando evento PaymentProcessed: {} en topic: {}", event.getEventId(), paymentsTopic);
        kafkaTemplate.send(paymentsTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error al publicar PaymentProcessed {}", event.getEventId(), ex);
                    } else {
                        log.info("PaymentProcessed {} publicado en partition {}, offset {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("Publicando evento PaymentFailed: {} en topic: {}", event.getEventId(), paymentsTopic);
        kafkaTemplate.send(paymentsTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error al publicar PaymentFailed {}", event.getEventId(), ex);
                    } else {
                        log.info("PaymentFailed {} publicado en partition {}, offset {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        log.info("Publicando evento PaymentRefunded: {} en topic: {}", event.getEventId(), paymentsTopic);
        kafkaTemplate.send(paymentsTopic, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error al publicar PaymentRefunded {}", event.getEventId(), ex);
                    } else {
                        log.info("PaymentRefunded {} publicado en partition {}, offset {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
