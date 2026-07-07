package com.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProcessedEvent {

    private UUID eventId;
    private UUID orderId;
    private String userId;
    private BigDecimal amount;
    private String transactionId;
    private String status;
    private List<OrderItemEvent> items;
    private Long timestamp;
}
