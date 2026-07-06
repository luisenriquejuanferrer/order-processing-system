package com.order.event;

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
public class OrderCreatedEvent {

    private UUID eventId;
    private UUID orderId;
    private String userId;
    private List<OrderItemEvent> items;
    private BigDecimal total;
    private Long timestamp;
}
