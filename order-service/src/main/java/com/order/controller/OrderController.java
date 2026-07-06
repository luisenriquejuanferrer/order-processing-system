package com.order.controller;

import com.order.dto.CreateOrderRequest;
import com.order.dto.OrderResponse;
import com.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Service", description = "API para gestionar pedidos")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Crear un nuevo pedido", description = "Crea un pedido y publica el evento OrderCreated en Kafka")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pedido por ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar todos los pedidos")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> response = orderService.getAllOrders();
        return ResponseEntity.ok(response);
    }
}
