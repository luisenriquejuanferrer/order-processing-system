package com.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.dto.CreateOrderRequest;
import com.order.dto.OrderItemRequest;
import com.order.dto.OrderResponse;
import com.order.model.OrderStatus;
import com.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_validRequest_returns201() throws Exception {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId("user1")
                .items(List.of(OrderItemRequest.builder()
                        .productId("p1")
                        .quantity(2)
                        .unitPrice(BigDecimal.TEN)
                        .build()))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(UUID.randomUUID())
                .userId("user1")
                .status(OrderStatus.PENDING)
                .total(new BigDecimal("20.00"))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .items(List.of())
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void createOrder_invalidRequest_returns400() throws Exception {
        // Given
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .userId("")
                .items(List.of())
                .build();

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrder_existingId_returns200() throws Exception {
        // Given
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .userId("user1")
                .status(OrderStatus.PENDING)
                .total(BigDecimal.TEN)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .items(List.of())
                .build();

        when(orderService.getOrder(orderId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }
}
