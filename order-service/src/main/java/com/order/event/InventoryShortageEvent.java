package com.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryShortageEvent {

    private UUID eventId;
    private UUID orderId;
    private String reason;
    private Long timestamp;
}
