package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.CreateProductRequest;
import com.inventory.dto.UpdateStockRequest;
import com.inventory.model.Product;
import com.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void createProduct_validRequest_returns201() throws Exception {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Producto de Prueba")
                .sku("SKU-001")
                .stock(100)
                .price(new BigDecimal("25.50"))
                .build();

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Producto de Prueba")
                .sku("SKU-001")
                .stock(100)
                .price(new BigDecimal("25.50"))
                .build();

        when(inventoryService.createProduct(anyString(), anyString(), anyInt(), any(BigDecimal.class)))
                .thenReturn(product);

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Producto de Prueba"))
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.stock").value(100))
                .andExpect(jsonPath("$.price").value(25.50));
    }

    @Test
    void createProduct_invalidRequest_returns400() throws Exception {
        // Given
        CreateProductRequest invalidRequest = CreateProductRequest.builder()
                .name("")
                .sku("")
                .stock(-1)
                .price(new BigDecimal("-10.00"))
                .build();

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProducts_returns200() throws Exception {
        // Given
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name("Producto de Prueba")
                .sku("SKU-001")
                .stock(100)
                .price(new BigDecimal("25.50"))
                .build();

        when(inventoryService.getAllProducts()).thenReturn(List.of(product));

        // When & Then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sku").value("SKU-001"));
    }

    @Test
    void updateStock_validRequest_returns200() throws Exception {
        // Given
        UUID productId = UUID.randomUUID();
        UpdateStockRequest request = UpdateStockRequest.builder()
                .stock(50)
                .build();

        Product product = Product.builder()
                .id(productId)
                .name("Producto de Prueba")
                .sku("SKU-001")
                .stock(50)
                .price(new BigDecimal("25.50"))
                .build();

        when(inventoryService.updateStock(productId, 50)).thenReturn(product);

        // When & Then
        mockMvc.perform(put("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(50));
    }
}
