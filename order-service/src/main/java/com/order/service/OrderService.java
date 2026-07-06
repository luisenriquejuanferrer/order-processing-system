package com.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.dto.*;
import com.order.event.OrderCreatedEvent;
import com.order.event.OrderItemEvent;
import com.order.model.Order;
import com.order.model.OrderItem;
import com.order.model.OrderStatus;
import com.order.model.OutboxEvent;
import com.order.model.OutboxStatus;
import com.order.repository.OrderRepository;
import com.order.repository.OutboxEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creando pedido para usuario: {}", request.getUserId());

        Order order = Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .build();

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build();
            order.addItem(item);
        }

        order.setTotal(calculateTotal(order));
        Order savedOrder = orderRepository.saveAndFlush(order);
        log.info("Pedido {} guardado en base de datos", savedOrder.getId());

        OrderCreatedEvent event = mapToEvent(savedOrder);
        saveOutboxEvent(savedOrder.getId(), event);
        log.info("Evento OrderCreated {} guardado en outbox", event.getEventId());

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        log.info("Buscando pedido por id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        log.info("Listando todos los pedidos");
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BigDecimal calculateTotal(Order order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderCreatedEvent mapToEvent(Order order) {
        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(order.getId())
                .userId(order.getUserId())
                .total(order.getTotal())
                .timestamp(Instant.now().toEpochMilli())
                .items(order.getItems().stream()
                        .map(item -> OrderItemEvent.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private void saveOutboxEvent(UUID orderId, OrderCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(orderId)
                    .eventType(event.getClass().getSimpleName())
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception ex) {
            log.error("Error al guardar evento en outbox para pedido {}", orderId, ex);
            throw new RuntimeException("No se pudo guardar el evento en outbox", ex);
        }
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
