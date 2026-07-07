package com.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.event.*;
import com.inventory.model.OutboxEvent;
import com.inventory.model.OutboxStatus;
import com.inventory.model.ProcessedEvent;
import com.inventory.model.Product;
import com.inventory.repository.OutboxEventRepository;
import com.inventory.repository.ProcessedEventRepository;
import com.inventory.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Transactional
    public Product createProduct(String name, String sku, Integer stock, BigDecimal price) {
        log.info("Creando producto: sku={}", sku);

        if (productRepository.findBySku(sku).isPresent()) {
            throw new IllegalArgumentException("Ya existe un producto con SKU: " + sku);
        }

        Product product = Product.builder()
                .name(name)
                .sku(sku)
                .stock(stock)
                .price(price)
                .build();

        Product saved = productRepository.saveAndFlush(product);
        log.info("Producto creado: id={}, sku={}", saved.getId(), saved.getSku());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product updateStock(UUID productId, Integer newStock) {
        log.info("Actualizando stock del producto: id={}", productId);

        if (newStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));

        product.setStock(newStock);
        Product saved = productRepository.saveAndFlush(product);
        log.info("Stock actualizado: id={}, stock={}", saved.getId(), saved.getStock());
        return saved;
    }

    @Transactional
    public void reserveStock(PaymentProcessedEvent event) {
        log.info("Reservando stock para pedido: {}", event.getOrderId());

        if (isEventAlreadyProcessed(event.getEventId().toString(), "PaymentProcessedEvent")) {
            log.warn("Evento PaymentProcessedEvent {} ya fue procesado. Ignorando.", event.getEventId());
            return;
        }

        List<Product> products = new ArrayList<>();
        List<OrderItemEvent> items = event.getItems();
        StringBuilder shortageReason = new StringBuilder();

        for (OrderItemEvent item : items) {
            Product product = productRepository.findById(UUID.fromString(item.getProductId())).orElse(null);

            if (product == null) {
                shortageReason.append("Producto no encontrado: ").append(item.getProductId()).append(". ");
                continue;
            }

            products.add(product);

            if (product.getStock() < item.getQuantity()) {
                shortageReason.append("Stock insuficiente para producto ")
                        .append(product.getSku())
                        .append(" (disponible: ")
                        .append(product.getStock())
                        .append(", requerido: ")
                        .append(item.getQuantity())
                        .append("). ");
            }
        }

        markEventAsProcessed(event.getEventId().toString(), "PaymentProcessedEvent");

        if (!shortageReason.isEmpty()) {
            for (Product product : products) {
                entityManager.detach(product);
            }

            log.warn("No se pudo reservar stock para pedido {}: {}", event.getOrderId(), shortageReason);

            InventoryShortageEvent shortageEvent = InventoryShortageEvent.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(event.getOrderId())
                    .reason(shortageReason.toString().trim())
                    .timestamp(Instant.now().toEpochMilli())
                    .build();

            saveOutboxEvent(event.getOrderId(), shortageEvent);
            return;
        }

        List<ReservedItemEvent> reservedItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            OrderItemEvent item = items.get(i);
            Product product = products.get(i);

            product.setStock(product.getStock() - item.getQuantity());
            reservedItems.add(ReservedItemEvent.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build());
        }

        productRepository.saveAllAndFlush(products);
        log.info("Stock reservado correctamente para pedido {}", event.getOrderId());

        InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(event.getOrderId())
                .status("RESERVED")
                .items(reservedItems)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        saveOutboxEvent(event.getOrderId(), reservedEvent);
    }

    private boolean isEventAlreadyProcessed(String eventId, String eventType) {
        return processedEventRepository.findByEventId(eventId).isPresent();
    }

    private void markEventAsProcessed(String eventId, String eventType) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .build();
        processedEventRepository.save(processedEvent);
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
}
