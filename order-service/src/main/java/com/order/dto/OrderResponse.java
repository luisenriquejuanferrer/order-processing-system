package com.order.dto;

import com.order.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID id;
    private String userId;
    private OrderStatus status;
    private BigDecimal total;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<OrderItemResponse> items;
}
