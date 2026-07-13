package com.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .description("API para gestión de inventario y productos del Order Processing System")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Order Processing System")
                                .email("dev@orderprocessing.local"))
                        .license(new License()
                                .name("MIT")));
    }
}
