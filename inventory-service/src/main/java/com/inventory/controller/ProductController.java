package com.inventory.controller;

import com.inventory.dto.CreateProductRequest;
import com.inventory.dto.ProductResponse;
import com.inventory.dto.UpdateStockRequest;
import com.inventory.model.Product;
import com.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Inventory Service", description = "API para gestionar productos e inventario")
public class ProductController {

    private final InventoryService inventoryService;

    @PostMapping
    @Operation(summary = "Crear un nuevo producto", description = "Crea un producto en el inventario con stock inicial")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = inventoryService.createProduct(
                request.getName(),
                request.getSku(),
                request.getStock(),
                request.getPrice()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(product));
    }

    @GetMapping
    @Operation(summary = "Listar todos los productos", description = "Obtiene la lista completa de productos con su stock actual")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = inventoryService.getAllProducts().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar stock de un producto", description = "Actualiza la cantidad de stock disponible para un producto específico")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStockRequest request) {
        Product product = inventoryService.updateStock(id, request.getStock());
        return ResponseEntity.ok(mapToResponse(product));
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .stock(product.getStock())
                .price(product.getPrice())
                .build();
    }
}
