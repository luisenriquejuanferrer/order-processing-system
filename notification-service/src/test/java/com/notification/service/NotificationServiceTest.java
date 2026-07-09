package com.notification.service;

import com.notification.event.InventoryShortageEvent;
import com.notification.event.OrderConfirmedEvent;
import com.notification.event.PaymentFailedEvent;
import com.notification.model.Notification;
import com.notification.model.NotificationType;
import com.notification.model.ProcessedEvent;
import com.notification.repository.NotificationRepository;
import com.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void handlePaymentFailed_success_createsNotification() {
        // Given
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .amount(new BigDecimal("127.50"))
                .reason("Pago rechazado")
                .timestamp(System.currentTimeMillis())
                .build();

        when(processedEventRepository.findByEventId(event.getEventId().toString())).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.handlePaymentFailed(event);

        // Then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getUserId()).isEqualTo("user1");
        assertThat(saved.getType()).isEqualTo(NotificationType.PAYMENT_FAILED);
        assertThat(saved.getMessage()).contains("Pago rechazado");
    }

    @Test
    void handleInventoryShortage_success_createsNotification() {
        // Given
        UUID orderId = UUID.randomUUID();
        InventoryShortageEvent event = InventoryShortageEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .reason("Stock insuficiente")
                .timestamp(System.currentTimeMillis())
                .build();

        when(processedEventRepository.findByEventId(event.getEventId().toString())).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.handleInventoryShortage(event);

        // Then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getUserId()).isEqualTo("unknown");
        assertThat(saved.getType()).isEqualTo(NotificationType.INVENTORY_SHORTAGE);
        assertThat(saved.getMessage()).contains("Stock insuficiente");
    }

    @Test
    void handleOrderConfirmed_success_createsNotification() {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId("user1")
                .timestamp(System.currentTimeMillis())
                .build();

        when(processedEventRepository.findByEventId(event.getEventId().toString())).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.handleOrderConfirmed(event);

        // Then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getUserId()).isEqualTo("user1");
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
    }

    @Test
    void handlePaymentFailed_duplicateEvent_ignored() {
        // Given
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId("user1")
                .amount(BigDecimal.TEN)
                .reason("Pago rechazado")
                .timestamp(System.currentTimeMillis())
                .build();

        when(processedEventRepository.findByEventId(event.getEventId().toString()))
                .thenReturn(Optional.of(ProcessedEvent.builder().build()));

        // When
        notificationService.handlePaymentFailed(event);

        // Then
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
