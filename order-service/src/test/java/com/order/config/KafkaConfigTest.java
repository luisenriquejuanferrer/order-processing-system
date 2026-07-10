package com.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = KafkaConfig.class)
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=localhost:9092")
class KafkaConfigTest {

    @Autowired
    private DefaultErrorHandler defaultErrorHandler;

    @Autowired
    private List<NewTopic> newTopics;

    @Test
    void defaultErrorHandler_shouldBeConfigured() {
        assertThat(defaultErrorHandler).isNotNull();
    }

    @Test
    void dltTopics_shouldBeCreated() {
        assertThat(newTopics)
                .extracting(NewTopic::name)
                .containsExactlyInAnyOrder("payments-dlt", "inventory-dlt");
    }
}
