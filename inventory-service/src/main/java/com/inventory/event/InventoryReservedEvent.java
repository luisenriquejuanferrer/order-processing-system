package com.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservedEvent {

    private UUID eventId;
    private UUID orderId;
    private String status;
    private List<ReservedItemEvent> items;
    private Long timestamp;
}
