package com.order.service;

import com.order.dto.CreateOrderRequest;
import com.order.dto.OrderItemRequest;
import com.order.dto.OrderResponse;
import com.order.event.OrderCreatedEvent;
import com.order.model.OutboxEvent;
import com.order.model.OutboxStatus;
import com.order.repository.OutboxEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"orders"})
@Disabled("Requiere Docker Desktop configurado para Testcontainers. " +
        "Habilitar tras configurar Docker Desktop → Settings → General → 'Expose daemon on tcp://localhost:2375 without TLS' " +
        "o tras configurar la variable de entorno DOCKER_HOST.")
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orderdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> postgres.getJdbcUrl() + "?currentSchema=orders");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // spring.kafka.bootstrap-servers se configura automaticamente por @EmbeddedKafka
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void createOrder_savesOrderAndOutboxEventAndPublishesToKafka() throws Exception {
        // Setup Kafka consumer
        BlockingQueue<ConsumerRecord<String, OrderCreatedEvent>> records = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.order.event");

        DefaultKafkaConsumerFactory<String, OrderCreatedEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("orders");
        KafkaMessageListenerContainer<String, OrderCreatedEvent> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        container.setupMessageListener((MessageListener<String, OrderCreatedEvent>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        try {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .userId("user1")
                    .items(List.of(OrderItemRequest.builder()
                            .productId("p1")
                            .quantity(2)
                            .unitPrice(BigDecimal.TEN)
                            .build()))
                    .build();

            // When
            OrderResponse response = orderService.createOrder(request);

            // Then - response
            assertThat(response.getUserId()).isEqualTo("user1");
            assertThat(response.getTotal()).isEqualTo(new BigDecimal("20.00"));
            assertThat(response.getStatus().name()).isEqualTo("PENDING");

            // Then - outbox event saved
            List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
            assertThat(outboxEvents).hasSize(1);
            assertThat(outboxEvents.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(outboxEvents.get(0).getAggregateId()).isEqualTo(response.getId());

            // Wait for OutboxPublisher to publish (runs every 5 seconds)
            ConsumerRecord<String, OrderCreatedEvent> record = records.poll(10, TimeUnit.SECONDS);
            assertThat(record).isNotNull();
            assertThat(record.value().getOrderId()).isEqualTo(response.getId());
            assertThat(record.value().getUserId()).isEqualTo("user1");
            assertThat(record.value().getTotal()).isEqualTo(new BigDecimal("20.00"));
        } finally {
            container.stop();
        }
    }
}
