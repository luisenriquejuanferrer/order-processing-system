package com.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.dto.CreateOrderRequest;
import com.order.dto.OrderItemRequest;
import com.order.dto.OrderResponse;
import com.order.event.OrderCreatedEvent;
import com.order.model.Order;
import com.order.model.OrderStatus;
import com.order.model.OutboxEvent;
import com.order.model.OutboxStatus;
import com.order.repository.OrderRepository;
import com.order.repository.OutboxEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        request = CreateOrderRequest.builder()
                .userId("user1")
                .items(List.of(OrderItemRequest.builder()
                        .productId("p1")
                        .quantity(2)
                        .unitPrice(BigDecimal.TEN)
                        .build()))
                .build();
    }

    @Test
    void createOrder_success_savesOrderAndOutboxEvent() throws Exception {
        // Given
        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId("user1")
                .status(OrderStatus.PENDING)
                .total(new BigDecimal("20.00"))
                .build();

        when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any(OrderCreatedEvent.class))).thenReturn("{}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrderResponse response = orderService.createOrder(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo("user1");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotal()).isEqualTo(new BigDecimal("20.00"));

        verify(orderRepository).saveAndFlush(any(Order.class));

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEvent savedEvent = outboxCaptor.getValue();
        assertThat(savedEvent.getAggregateId()).isEqualTo(savedOrder.getId());
        assertThat(savedEvent.getEventType()).isEqualTo("OrderCreatedEvent");
        assertThat(savedEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void getOrder_success_returnsOrder() {
        // Given
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .userId("user1")
                .status(OrderStatus.PENDING)
                .total(BigDecimal.TEN)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        OrderResponse response = orderService.getOrder(orderId);

        // Then
        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getUserId()).isEqualTo("user1");
    }

    @Test
    void getOrder_notFound_throwsException() {
        // Given
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> orderService.getOrder(orderId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Pedido no encontrado");
    }

    @Test
    void getAllOrders_success_returnsList() {
        // Given
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId("user1")
                .status(OrderStatus.PENDING)
                .total(BigDecimal.TEN)
                .build();

        when(orderRepository.findAll()).thenReturn(List.of(order));

        // When
        List<OrderResponse> response = orderService.getAllOrders();

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getUserId()).isEqualTo("user1");
    }

    @Test
    void createOrder_emptyItems_savesOrderWithZeroTotal() throws Exception {
        // Given
        CreateOrderRequest emptyRequest = CreateOrderRequest.builder()
                .userId("user1")
                .items(Collections.emptyList())
                .build();

        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId("user1")
                .status(OrderStatus.PENDING)
                .total(BigDecimal.ZERO)
                .build();

        when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any(OrderCreatedEvent.class))).thenReturn("{}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        orderService.createOrder(emptyRequest);

        // Then
        verify(orderRepository).saveAndFlush(any(Order.class));
    }
}
