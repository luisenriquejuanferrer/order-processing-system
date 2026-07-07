package com.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.event.InventoryReservedEvent;
import com.inventory.event.InventoryShortageEvent;
import com.inventory.event.OrderItemEvent;
import com.inventory.event.PaymentProcessedEvent;
import com.inventory.model.OutboxEvent;
import com.inventory.model.OutboxStatus;
import com.inventory.model.ProcessedEvent;
import com.inventory.model.Product;
import com.inventory.repository.OutboxEventRepository;
import com.inventory.repository.ProcessedEventRepository;
import com.inventory.repository.ProductRepository;
import jakarta.persistence.EntityManager;
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
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;
    private Product product;
    private UUID orderId;
    private UUID eventId;
    private PaymentProcessedEvent paymentProcessedEvent;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        product = Product.builder()
                .id(productId)
                .name("Producto de Prueba")
                .sku("SKU-001")
                .stock(10)
                .price(new BigDecimal("25.50"))
                .build();

        paymentProcessedEvent = PaymentProcessedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .userId("user1")
                .amount(new BigDecimal("51.00"))
                .transactionId("TXN-12345678")
                .status("APPROVED")
                .items(List.of(OrderItemEvent.builder()
                        .productId(productId.toString())
                        .quantity(2)
                        .unitPrice(new BigDecimal("25.50"))
                        .build()))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Test
    void createProduct_success_savesProduct() {
        // Given
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(product);

        // When
        Product result = inventoryService.createProduct("Producto de Prueba", "SKU-001", 10, new BigDecimal("25.50"));

        // Then
        assertThat(result.getSku()).isEqualTo("SKU-001");
        verify(productRepository).saveAndFlush(any(Product.class));
    }

    @Test
    void createProduct_duplicateSku_throwsException() {
        // Given
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));

        // Then
        assertThatThrownBy(() -> inventoryService.createProduct("Producto de Prueba", "SKU-001", 10, new BigDecimal("25.50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un producto con SKU");

        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void updateStock_success_updatesProductStock() {
        // Given
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(product);

        // When
        Product result = inventoryService.updateStock(productId, 5);

        // Then
        assertThat(result.getStock()).isEqualTo(5);
        verify(productRepository).saveAndFlush(any(Product.class));
    }

    @Test
    void updateStock_productNotFound_throwsException() {
        // Given
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> inventoryService.updateStock(productId, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Producto no encontrado");
    }

    @Test
    void updateStock_negativeStock_throwsException() {
        // Then
        assertThatThrownBy(() -> inventoryService.updateStock(productId, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El stock no puede ser negativo");

        verifyNoInteractions(productRepository);
    }

    @Test
    void reserveStock_sufficientStock_decrementsAndSavesReservedEvent() throws Exception {
        // Given
        when(processedEventRepository.findByEventId(eventId.toString())).thenReturn(Optional.empty());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.saveAllAndFlush(anyList())).thenReturn(List.of(product));
        when(objectMapper.writeValueAsString(any(InventoryReservedEvent.class))).thenReturn("{}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        inventoryService.reserveStock(paymentProcessedEvent);

        // Then
        assertThat(product.getStock()).isEqualTo(8);
        verify(productRepository).saveAllAndFlush(anyList());
        verify(entityManager, never()).detach(any(Product.class));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("InventoryReservedEvent");
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void reserveStock_insufficientStock_publishesShortageEventAndDetachesProducts() throws Exception {
        // Given
        product.setStock(1);
        when(processedEventRepository.findByEventId(eventId.toString())).thenReturn(Optional.empty());
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(objectMapper.writeValueAsString(any(InventoryShortageEvent.class))).thenReturn("{}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        inventoryService.reserveStock(paymentProcessedEvent);

        // Then
        assertThat(product.getStock()).isEqualTo(1);
        verify(productRepository, never()).saveAllAndFlush(anyList());
        verify(entityManager).detach(product);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("InventoryShortageEvent");
    }

    @Test
    void reserveStock_productNotFound_publishesShortageEvent() throws Exception {
        // Given
        when(processedEventRepository.findByEventId(eventId.toString())).thenReturn(Optional.empty());
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any(InventoryShortageEvent.class))).thenReturn("{}");
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        inventoryService.reserveStock(paymentProcessedEvent);

        // Then
        verify(productRepository, never()).saveAllAndFlush(anyList());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("InventoryShortageEvent");
    }

    @Test
    void reserveStock_duplicateEvent_ignored() {
        // Given
        when(processedEventRepository.findByEventId(eventId.toString()))
                .thenReturn(Optional.of(ProcessedEvent.builder().build()));

        // When
        inventoryService.reserveStock(paymentProcessedEvent);

        // Then
        verify(productRepository, never()).findById(any(UUID.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }
}
