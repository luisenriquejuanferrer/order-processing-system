package com.payment.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterConsumer {

    @KafkaListener(topics = {"orders-dlt", "inventory-dlt"}, groupId = "payment-service-dlt")
    public void handleDeadLetter(ConsumerRecord<String, String> record) {
        String errorMessage = extractErrorMessage(record);
        log.error("Mensaje recibido en DLT - topic: {}, partition: {}, offset: {}, key: {}, value: {}, error: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value(),
                errorMessage);
    }

    private String extractErrorMessage(ConsumerRecord<String, String> record) {
        org.apache.kafka.common.header.Header header = record.headers().lastHeader("kafka_dlt-exception-fqcn");
        if (header != null) {
            return new String(header.value());
        }
        return "unknown";
    }
}
