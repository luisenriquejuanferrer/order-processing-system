package com.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservedItemEvent {

    private String productId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
