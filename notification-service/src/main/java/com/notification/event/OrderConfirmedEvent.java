package com.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderConfirmedEvent {

    private UUID eventId;
    private UUID orderId;
    private String userId;
    private Long timestamp;
}
