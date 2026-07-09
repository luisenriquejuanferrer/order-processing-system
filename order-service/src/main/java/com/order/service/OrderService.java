package com.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.dto.*;
import com.order.event.*;
import com.order.model.Order;
import com.order.model.OrderItem;
import com.order.model.OrderStatus;
import com.order.model.OutboxEvent;
import com.order.model.OutboxStatus;
import com.order.model.ProcessedEvent;
import com.order.repository.OrderRepository;
import com.order.repository.OutboxEventRepository;
import com.order.repository.ProcessedEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
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

        OrderCreatedEvent event = mapToOrderCreatedEvent(savedOrder);
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

    @Transactional
    public void markAsConfirmed(UUID orderId) {
        log.info("Marcando pedido {} como CONFIRMED", orderId);
        updateOrderStatus(orderId, OrderStatus.CONFIRMED, "El pedido ha sido confirmado");
    }

    @Transactional
    public void markAsFailed(UUID orderId, String reason) {
        log.info("Marcando pedido {} como FAILED. Motivo: {}", orderId, reason);
        updateOrderStatus(orderId, OrderStatus.FAILED, reason);
    }

    @Transactional
    public void markAsCancelled(UUID orderId, String reason) {
        log.info("Marcando pedido {} como CANCELLED. Motivo: {}", orderId, reason);
        updateOrderStatus(orderId, OrderStatus.CANCELLED, reason);
    }

    private void updateOrderStatus(UUID orderId, OrderStatus newStatus, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + orderId));

        if (!canTransition(order.getStatus(), newStatus)) {
            log.warn("Transición de estado no permitida para pedido {}: {} -> {}", orderId, order.getStatus(), newStatus);
            return;
        }

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.saveAndFlush(order);
        log.info("Pedido {} actualizado a estado {}", orderId, newStatus);

        if (newStatus == OrderStatus.CONFIRMED) {
            OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(savedOrder.getId())
                    .userId(savedOrder.getUserId())
                    .timestamp(Instant.now().toEpochMilli())
                    .build();
            saveOutboxEvent(savedOrder.getId(), event);
            log.info("Evento OrderConfirmed {} guardado en outbox", event.getEventId());
        }
    }

    private boolean canTransition(OrderStatus current, OrderStatus next) {
        if (current == OrderStatus.PENDING) {
            return next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED || next == OrderStatus.FAILED;
        }
        return false;
    }

    private BigDecimal calculateTotal(Order order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderCreatedEvent mapToOrderCreatedEvent(Order order) {
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
