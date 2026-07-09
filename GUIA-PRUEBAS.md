# Guía de Pruebas - Order Processing System

## Requisitos Previos

- Docker Desktop ejecutándose
- Java 21 instalado
- Maven 3.9+ instalado
- IntelliJ IDEA (opcional, para ejecutar servicios)

## Paso 1: Arrancar Todo el Sistema

Con un solo comando se levanta Kafka, PostgreSQL, Prometheus, Grafana y los 4 microservicios:

```bash
# Construir imágenes y arrancar todos los contenedores
docker compose up -d --build

# Verificar que todos los contenedores están running/healthy
docker compose ps
```

Deberías ver 8 contenedores con estado "Up" o "healthy":
- kafka
- postgres
- prometheus
- grafana
- order-service
- payment-service
- inventory-service
- notification-service

## Paso 2: Verificar Logs

```bash
# Logs de todos los servicios
docker compose logs -f

# Logs de un servicio específico
docker compose logs -f order-service
```

## Paso 3: Arrancar Servicios para Desarrollo (alternativa)

Si prefieres desarrollar con IntelliJ, primero levanta la infraestructura:

```bash
docker compose up -d kafka postgres prometheus grafana
```

Luego ejecuta las aplicaciones en este orden:

1. **OrderServiceApplication** (puerto 8081)
2. **PaymentServiceApplication** (puerto 8082)
3. **InventoryServiceApplication** (puerto 8083)
4. **NotificationServiceApplication** (puerto 8084)

O desde terminal:

```bash
# Terminal 1 - Order Service
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar

# Terminal 2 - Payment Service
java -jar payment-service/target/payment-service-1.0.0-SNAPSHOT.jar

# Terminal 3 - Inventory Service
java -jar inventory-service/target/inventory-service-1.0.0-SNAPSHOT.jar

# Terminal 4 - Notification Service
java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar
```

## Paso 4: Importar Collection en Postman

1. Abre Postman
2. Click en **Import**
3. Selecciona el archivo `postman-collection.json`
4. La collection "Order Processing System" aparecerá en tu workspace

## Paso 5: Crear Productos en el Inventario

Antes de crear pedidos, necesitas productos en el inventario.

### 5.1 Crear un Producto

- **Request:** `Inventory Service > Crear Producto`
- **Método:** POST
- **URL:** `http://localhost:8083/api/products`
- **Body:**
```json
{
  "name": "Producto de Prueba",
  "sku": "SKU-001",
  "stock": 100,
  "price": 25.50
}
```

- **Respuesta esperada:** 201 Created
```json
{
  "id": "uuid-del-producto",
  "name": "Producto de Prueba",
  "sku": "SKU-001",
  "stock": 100,
  "price": 25.50
}
```

**IMPORTANTE:** Copia el `id` del producto para usarlo en los siguientes pasos.

### 5.2 Listar Productos

- **Request:** `Inventory Service > Listar Productos`
- **Método:** GET
- **URL:** `http://localhost:8083/api/products`
- **Respuesta esperada:** 200 OK con lista de productos

## Paso 6: Probar el Flujo Completo

### 6.1 Crear un Pedido

- **Request:** `Order Service > Crear Pedido`
- **Método:** POST
- **URL:** `http://localhost:8081/api/orders`
- **Body:**
```json
{
  "userId": "user1",
  "items": [
    {
      "productId": "REEMPLAZAR_CON_UUID_DEL_PRODUCTO",
      "quantity": 2,
      "unitPrice": 25.50
    }
  ]
}
```

**IMPORTANTE:** Reemplaza `REEMPLAZAR_CON_UUID_DEL_PRODUCTO` con el UUID del producto que creaste en el Paso 5.

- **Respuesta esperada:** 201 Created
```json
{
  "id": "uuid-del-pedido",
  "userId": "user1",
  "status": "PENDING",
  "total": 20.00,
  "createdAt": "2026-07-06T...",
  "updatedAt": "2026-07-06T...",
  "items": [...]
}
```

**IMPORTANTE:** Copia el `id` del pedido para usarlo en los siguientes pasos.

### 6.2 Verificar el Pago

- **Request:** `Payment Service > Obtener Pago por Order ID`
- **Método:** GET
- **URL:** `http://localhost:8082/api/payments/order/{orderId}`
- Reemplaza `{orderId}` con el ID copiado en el paso anterior

- **Respuesta esperada:** 200 OK
```json
{
  "id": "uuid-del-pago",
  "orderId": "uuid-del-pedido",
  "userId": "user1",
  "amount": 20.00,
  "status": "APPROVED",  // o "DECLINED" (80% probabilidad de aprobación)
  "transactionId": "TXN-XXXXXXXX",
  "createdAt": "2026-07-06T..."
}
```

### 6.3 Verificar Eventos en Kafka

```bash
# Ver eventos del topic "orders"
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9093 \
  --topic orders \
  --from-beginning

# Ver eventos del topic "payments"
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9093 \
  --topic payments \
  --from-beginning

# Ver eventos del topic "inventory"
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9093 \
  --topic inventory \
  --from-beginning
```

Deberías ver:
- En topic `orders`: `OrderCreatedEvent` con los datos del pedido
- En topic `payments`: `PaymentProcessedEvent` o `PaymentFailedEvent` según el resultado
- En topic `inventory`: `InventoryReservedEvent` o `InventoryShortageEvent` según el stock disponible

### 6.4 Verificar en Base de Datos

```bash
# Conectar a PostgreSQL
docker exec -it postgres psql -U postgres -d orderdb

# Ver pedidos
SELECT * FROM orders.orders;

# Ver items de pedidos
SELECT * FROM orders.order_items;

# Ver pagos
SELECT * FROM payments.payments;

# Ver productos
SELECT * FROM inventory.products;

# Ver eventos del outbox (Order Service)
SELECT * FROM orders.outbox_events;

# Ver eventos del outbox (Payment Service)
SELECT * FROM payments.outbox_events;

# Ver eventos del outbox (Inventory Service)
SELECT * FROM inventory.outbox_events;

# Ver eventos procesados (Inventory Service)
SELECT * FROM inventory.processed_events;

# Ver notificaciones
SELECT * FROM notifications.notifications;

# Ver eventos procesados (Notification Service)
SELECT * FROM notifications.processed_events;

# Salir de psql
\q
```

## Paso 7: Verificar Health Checks

- **Request:** `Actuator > Order Service Health`
- **URL:** `http://localhost:8081/actuator/health`
- **Respuesta esperada:** `{"status": "UP"}`

- **Request:** `Actuator > Payment Service Health`
- **URL:** `http://localhost:8082/actuator/health`
- **Respuesta esperada:** `{"status": "UP"}`

- **Request:** `Actuator > Inventory Service Health`
- **URL:** `http://localhost:8083/actuator/health`
- **Respuesta esperada:** `{"status": "UP"}`

- **Request:** `Actuator > Notification Service Health`
- **URL:** `http://localhost:8084/actuator/health`
- **Respuesta esperada:** `{"status": "UP"}`

## Paso 8: Verificar Notificaciones

El Notification Service consume eventos de pago fallido y stock insuficiente. Crea notificaciones en la base de datos automáticamente.

### 8.1 Listar Todas las Notificaciones

- **Request:** `Notification Service > Listar Notificaciones`
- **Método:** GET
- **URL:** `http://localhost:8084/api/notifications`
- **Respuesta esperada:** 200 OK con lista de notificaciones

### 8.2 Listar Notificaciones por Pedido

- **Request:** `Notification Service > Notificaciones por Pedido`
- **Método:** GET
- **URL:** `http://localhost:8084/api/notifications/order/{orderId}`
- Reemplaza `{orderId}` con el ID del pedido
- **Respuesta esperada:** 200 OK con notificaciones del pedido

## Paso 9: Verificar Swagger UI

- **Request:** `Swagger UI > Order Service Swagger`
- **URL:** `http://localhost:8081/swagger-ui.html`
- Deberías ver la documentación interactiva de la API

## Paso 10: Probar Escenarios de Error

### 9.1 Pedido con Items Vacíos

```json
{
  "userId": "user1",
  "items": []
}
```

**Respuesta esperada:** 400 Bad Request

### 9.2 Pedido sin UserId

```json
{
  "userId": "",
  "items": [
    {
      "productId": "REEMPLAZAR_CON_UUID_DEL_PRODUCTO",
      "quantity": 2,
      "unitPrice": 25.50
    }
  ]
}
```

**Respuesta esperada:** 400 Bad Request

### 9.3 Producto con SKU Duplicado

```json
{
  "name": "Otro Producto",
  "sku": "SKU-001",
  "stock": 50,
  "price": 15.00
}
```

**Respuesta esperada:** 500 Internal Server Error (ya existe un producto con ese SKU)

### 9.4 Producto con Stock Negativo

```json
{
  "name": "Producto Inválido",
  "sku": "SKU-002",
  "stock": -10,
  "price": 25.50
}
```

**Respuesta esperada:** 400 Bad Request

### 9.5 Pedido con Stock Insuficiente

Primero, actualiza el stock de un producto a un valor bajo:

```bash
# Actualizar stock a 1
PUT http://localhost:8083/api/products/{productId}
{
  "stock": 1
}
```

Luego, crea un pedido con quantity mayor al stock disponible:

```json
{
  "userId": "user1",
  "items": [
    {
      "productId": "REEMPLAZAR_CON_UUID_DEL_PRODUCTO",
      "quantity": 5,
      "unitPrice": 25.50
    }
  ]
}
```

**Flujo esperado:**
1. Order Service crea el pedido (status: PENDING)
2. Payment Service procesa el pago (80% probabilidad de aprobación)
3. Inventory Service recibe PaymentProcessedEvent
4. Inventory Service detecta stock insuficiente
5. Inventory Service publica InventoryShortageEvent
6. Payment Service recibe InventoryShortageEvent y emite PaymentRefundedEvent
7. Order Service recibe eventos y marca el pedido como CANCELLED

## Paso 11: Monitoreo con Prometheus y Grafana

- **Prometheus:** http://localhost:9090
  - Ver métricas de los servicios
  - Ejecutar queries como `http_server_requests_seconds_count`

- **Grafana:** http://localhost:3000
  - Usuario: `admin`
  - Password: `admin`
  - Explorar dashboards preconfigurados

## Flujo de Eventos Esperado

```
1. Cliente → POST /api/orders
2. Order Service → Guarda pedido en BD (status: PENDING)
3. Order Service → Guarda evento en outbox_events (status: PENDING)
4. Order Service → Responde 201 Created
5. OutboxPublisher (Order) → Lee eventos PENDING
6. OutboxPublisher (Order) → Publica OrderCreatedEvent en topic "orders"
7. OutboxPublisher (Order) → Marca evento como PUBLISHED
8. Payment Service → Consume OrderCreatedEvent
9. Payment Service → Procesa pago (80% APPROVED, 20% DECLINED)
10. Payment Service → Guarda pago en BD
11. Payment Service → Guarda evento en outbox_events
12. OutboxPublisher (Payment) → Publica PaymentProcessedEvent en topic "payments"
13. Inventory Service → Consume PaymentProcessedEvent
14. Inventory Service → Verifica stock disponible
15. Inventory Service → Si stock suficiente: decrementa stock, publica InventoryReservedEvent
16. Inventory Service → Si stock insuficiente: publica InventoryShortageEvent
17. Order Service → Consume InventoryReservedEvent → marca pedido como CONFIRMED
18. Order Service → Guarda OrderConfirmedEvent en outbox
19. OutboxPublisher (Order) → Publica OrderConfirmedEvent en topic "orders"
20. Notification Service → Consume OrderConfirmedEvent → crea notificación ORDER_CONFIRMED
21. Order Service → Consume InventoryShortageEvent → marca pedido como CANCELLED
22. Payment Service → Consume InventoryShortageEvent → emite PaymentRefundedEvent (compensación)
23. Notification Service → Consume PaymentFailedEvent → crea notificación PAYMENT_FAILED
24. Notification Service → Consume InventoryShortageEvent → crea notificación INVENTORY_SHORTAGE
```

## Comandos Útiles

```bash
# Ver logs de todos los contenedores
docker compose logs -f

# Ver logs solo de Kafka
docker compose logs -f kafka

# Ver topics de Kafka
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9093 \
  --list

# Ver detalles de un topic
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9093 \
  --describe \
  --topic orders

# Resetear offset de un consumer group (para re-procesar mensajes)
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9093 \
  --group payment-service \
  --reset-offsets \
  --to-earliest \
  --execute \
  --topic orders

# Resetear offset para inventory-service
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9093 \
  --group inventory-service \
  --reset-offsets \
  --to-earliest \
  --execute \
  --topic payments
```

## Troubleshooting

### Error: "Connection refused" en PostgreSQL
- Verifica que PostgreSQL está running: `docker compose ps`
- Verifica que el puerto 5432 no esté ocupado

### Error: "UnknownHostException: kafka"
- Verifica que Kafka está running: `docker compose ps`
- Asegúrate de que `application.yml` usa `localhost:9093` para desarrollo local

### Error: "Class not found" en Payment Service
- Verifica que el consumer usa `StringDeserializer`
- Verifica que `PaymentEventConsumer` deserializa manualmente con `ObjectMapper`

### Los eventos no llegan a Kafka
- Verifica que `OutboxPublisher` está ejecutándose (logs cada 5 segundos)
- Verifica que hay eventos con status `PENDING` en `outbox_events`

### Payment Service no procesa el pago
- Verifica que el topic `orders` tiene mensajes
- Verifica los logs de Payment Service
- Verifica que el consumer group `payment-service` está configurado correctamente

### Inventory Service no reserva stock
- Verifica que el topic `payments` tiene mensajes
- Verifica los logs de Inventory Service
- Verifica que el producto existe en `inventory.products` con stock suficiente
- Verifica que el `productId` en el pedido coincide con el UUID del producto en el inventario
- Verifica que el consumer group `inventory-service` está configurado correctamente

### Stock no se decrementa después de un pedido
- Verifica que el PaymentProcessedEvent incluye los items del pedido
- Verifica que el OutboxPublisher de Inventory Service está ejecutándose
- Verifica la tabla `inventory.processed_events` para confirmar que el evento fue procesado
- Verifica la tabla `inventory.outbox_events` para ver si el evento InventoryReservedEvent fue generado

## Paso 12: Limpiar y Reiniciar

```bash
# Parar todos los contenedores
docker compose down

# Parar y eliminar volúmenes (borra datos de PostgreSQL)
docker compose down -v

# Reconstruir y arrancar todo de nuevo
docker compose up -d --build
```
