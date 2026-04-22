# INFORME TECNICO - SmartLogix

## 1. Identificacion del proyecto

- Nombre: SmartLogix - Plataforma Inteligente para Gestion Logistica (Microservicios)
- Version: 1.0.0
- Tipo: sistema distribuido basado en microservicios con Spring Boot + Spring Cloud
- Repositorio raiz: `smartlogix-platform`

## 2. Objetivo

Este documento describe la arquitectura, los patrones de diseno implementados, los modulos funcionales, los endpoints expuestos, el flujo operacional principal y las opciones de despliegue del proyecto SmartLogix. El alcance corresponde a la base del Parcial 1 y deja una hoja de ruta para Parcial 2/3.

## 3. Alcance

Incluye:

- Servicios de negocio: `inventory-service`, `order-service`, `shipment-service`
- Servicios de infraestructura: `discovery-service`, `api-gateway`
- Comunicacion sincrona entre microservicios con descubrimiento dinamico
- Persistencia local por servicio (H2 en memoria)

No incluye en esta version:

- Autenticacion/autorizacion JWT
- Trazabilidad distribuida
- Mensajeria asincrona (Kafka/RabbitMQ)
- Frontend web

## 4. Arquitectura general

### 4.1 Topologia

- `discovery-service` (Eureka Server) en puerto `8761`
- `api-gateway` (Spring Cloud Gateway) en puerto `8080`
- `inventory-service` en puerto `8081`
- `order-service` en puerto `8082`
- `shipment-service` en puerto `8083`

Referencia de modulos: `pom.xml` (raiz) secciones `<modules>` y properties de version.

### 4.2 Tecnologias principales

- Java 17
- Spring Boot 3.3.5
- Spring Cloud 2023.0.3
- Spring Data JPA
- Eureka (server/client)
- Spring Cloud Gateway
- Resilience4j (circuit breaker)
- Base de datos H2 por servicio
- Docker + Docker Compose

Referencias:

- `pom.xml` (raiz)
- `*/pom.xml` por modulo

### 4.3 Principios aplicados

- Separacion de responsabilidades por dominio
- Acoplamiento bajo mediante llamadas HTTP entre servicios
- Punto unico de entrada por API Gateway
- Descubrimiento dinamico de instancias via Eureka
- Resiliencia en llamadas criticas de `order-service` a `shipment-service`

## 5. Patrones de arquitectura y diseno implementados

### 5.1 Service Discovery

- `discovery-service` habilita Eureka Server con `@EnableEurekaServer`.
- Los demas servicios se registran como clientes en `defaultZone`.

Evidencia:

- `discovery-service/src/main/java/com/smartlogix/discovery/DiscoveryServiceApplication.java`
- `*/src/main/resources/application.yml` (`eureka.client.service-url.defaultZone`)

### 5.2 API Gateway

- El gateway enruta por prefijos de path hacia servicios logicos (`lb://...`).
- Reglas CORS globales habilitadas para simplificar consumo cliente.

Evidencia:

- `api-gateway/src/main/resources/application.yml` (`spring.cloud.gateway.routes`)

### 5.3 Database per Service

Cada microservicio de negocio mantiene su propia base H2 en memoria:

- `inventorydb`
- `orderdb`
- `shipmentdb`

Evidencia:

- `inventory-service/src/main/resources/application.yml`
- `order-service/src/main/resources/application.yml`
- `shipment-service/src/main/resources/application.yml`

### 5.4 Factory Method (shipment-service)

`ShipmentService` delega la generacion de plan de envio a una fabrica concreta segun la direccion normalizada:

- `SouthernShipmentPlanFactory`
- `NorthernShipmentPlanFactory`
- `CentralShipmentPlanFactory` (fallback por defecto)

La seleccion se resuelve en `ShipmentPlanFactoryResolver` usando lista de fabricas inyectadas.

Evidencia:

- `shipment-service/src/main/java/com/smartlogix/shipment/factory/ShipmentPlanFactory.java`
- `shipment-service/src/main/java/com/smartlogix/shipment/factory/ShipmentPlanFactoryResolver.java`
- `shipment-service/src/main/java/com/smartlogix/shipment/factory/*ShipmentPlanFactory.java`

### 5.5 Circuit Breaker (order-service -> shipment-service)

`ShipmentClient` encapsula la llamada HTTP de creacion de envio dentro de un circuit breaker llamado `shipmentService`. En caso de falla, retorna un fallback con estado manual.

Evidencia:

- `order-service/src/main/java/com/smartlogix/order/client/ShipmentClient.java`
- `order-service/src/main/resources/application.yml` (`resilience4j.circuitbreaker.instances.shipmentService`)

### 5.6 Orquestacion sincrona

`OrderService` implementa una orquestacion secuencial:

1. Crea y persiste orden en estado inicial.
2. Verifica disponibilidad por cada linea contra inventario.
3. Reserva stock por linea.
4. Solicita envio a `shipment-service`.
5. Actualiza estado final (`SHIPMENT_REQUESTED`, `REJECTED` o `FAILED`).

Evidencia:

- `order-service/src/main/java/com/smartlogix/order/service/OrderService.java`

## 6. Descripcion de modulos

### 6.1 discovery-service

Responsabilidad:

- Registro y descubrimiento de servicios.

Aspectos tecnicos:

- Eureka Server activo
- Actuator para `health` e `info`

### 6.2 api-gateway

Responsabilidad:

- Entrada unica para clientes externos.

Rutas:

- `/api/inventory/**` -> `inventory-service`
- `/api/orders/**` -> `order-service`
- `/api/shipments/**` -> `shipment-service`

### 6.3 inventory-service

Responsabilidad:

- Gestionar SKUs, stock disponible, reserva, liberacion y despacho.

Aspectos relevantes:

- Seed inicial de inventario en `InventorySeedConfig`
- Validaciones de negocio para cantidades y stock
- Excepciones de dominio con respuestas HTTP estructuradas

### 6.4 order-service

Responsabilidad:

- Gestion de pedidos y coordinacion de inventario + envio.

Aspectos relevantes:

- Cliente HTTP load-balanced (`@LoadBalanced RestTemplate`)
- Integracion con `inventory-service` y `shipment-service`
- Circuit breaker con fallback para degradacion controlada

### 6.5 shipment-service

Responsabilidad:

- Generar envios, tracking, ruta, carrier, fecha estimada y estado.

Aspectos relevantes:

- Planificacion por zona usando Factory Method
- Tracking code generado con prefijo `SLX-`
- Estados: `PLANNED`, `PICKED_UP`, `IN_TRANSIT`, `DELIVERED`

## 7. Endpoints clave

### 7.1 Inventory Service

- `GET /api/inventory/items`
- `POST /api/inventory/items`
- `GET /api/inventory/items/{sku}`
- `GET /api/inventory/items/{sku}/availability?quantity=...`
- `PATCH|POST /api/inventory/items/{sku}/reserve?quantity=...`
- `PATCH|POST /api/inventory/items/{sku}/release?quantity=...`
- `PATCH|POST /api/inventory/items/{sku}/dispatch?quantity=...`

Evidencia: `inventory-service/src/main/java/com/smartlogix/inventory/controller/InventoryController.java`

### 7.2 Order Service

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderNumber}`

Evidencia: `order-service/src/main/java/com/smartlogix/order/controller/OrderController.java`

### 7.3 Shipment Service

- `POST /api/shipments`
- `GET /api/shipments`
- `GET /api/shipments/{trackingCode}`
- `PATCH /api/shipments/{trackingCode}/status?value=IN_TRANSIT`

Evidencia: `shipment-service/src/main/java/com/smartlogix/shipment/controller/ShipmentController.java`

## 8. Flujo funcional principal (crear pedido)

1. Cliente envia `POST /api/orders` por gateway.
2. `order-service` construye y guarda orden (`PENDING`).
3. Por cada linea, consulta disponibilidad en `inventory-service`.
4. Si no hay stock suficiente, orden pasa a `REJECTED` con razon.
5. Si hay stock, reserva unidades por SKU.
6. Si falla alguna reserva, libera reservas previas y marca `REJECTED`.
7. Si reserva completa, solicita envio a `shipment-service`.
8. Si no hay respuesta de envio, orden queda `FAILED` (asignacion manual).
9. Si envio exitoso, orden queda `SHIPMENT_REQUESTED` con `trackingCode`.

Evidencia: `order-service/src/main/java/com/smartlogix/order/service/OrderService.java`

## 9. Modelo de datos y estados

### 9.1 Orden

Entidad `PurchaseOrder`:

- `orderNumber` unico autogenerado (`ORD-...`)
- datos cliente y direccion
- `status`, `totalAmount`, `trackingCode`, `rejectionReason`, `createdAt`
- relacion con lineas (`OrderLine`)

Estados posibles (`OrderStatus`):

- `PENDING`
- `APPROVED`
- `REJECTED`
- `SHIPMENT_REQUESTED`
- `FAILED`

### 9.2 Envio

Entidad `Shipment`:

- `trackingCode` unico (`SLX-...`)
- `orderNumber`, direccion destino, unidades totales
- `carrier`, `routeCode`, fecha estimada, `status`, `createdAt`

Estados posibles (`ShipmentStatus`):

- `PLANNED`
- `PICKED_UP`
- `IN_TRANSIT`
- `DELIVERED`

## 10. Manejo de errores y validaciones

Los servicios de negocio exponen un `GlobalExceptionHandler` con formato uniforme:

- timestamp
- status HTTP
- mensaje

Casos cubiertos:

- recursos no encontrados (`404`)
- validaciones de entrada (`400`)
- errores de negocio (`400`, en inventario)
- errores no controlados (`500`)

Evidencia:

- `inventory-service/src/main/java/com/smartlogix/inventory/exception/GlobalExceptionHandler.java`
- `order-service/src/main/java/com/smartlogix/order/exception/GlobalExceptionHandler.java`
- `shipment-service/src/main/java/com/smartlogix/shipment/exception/GlobalExceptionHandler.java`

## 11. Ejecucion y despliegue

### 11.1 Requisitos

- Java 17
- Maven Wrapper (`mvnw.cmd`)
- Docker Desktop (opcional)

### 11.2 Compilacion y pruebas

Desde la raiz:

```powershell
.\mvnw.cmd clean test
```

### 11.3 Ejecucion manual por modulo

```powershell
.\mvnw.cmd -pl discovery-service spring-boot:run
.\mvnw.cmd -pl inventory-service spring-boot:run
.\mvnw.cmd -pl shipment-service spring-boot:run
.\mvnw.cmd -pl order-service spring-boot:run
.\mvnw.cmd -pl api-gateway spring-boot:run
```

### 11.4 Ejecucion por script

```powershell
.\run-services.ps1
```

### 11.5 Despliegue Docker Compose

```powershell
docker compose up --build -d
docker compose ps
```

```powershell
docker compose down
```

Scripts de apoyo:

- `run-docker.ps1` valida daemon y luego ejecuta compose.
- `run-services.ps1` abre una consola por servicio y ejecuta Spring Boot.

## 12. Verificacion funcional rapida

Con plataforma arriba, validar via gateway (`http://localhost:8080`):

1. Listar inventario:

```powershell
curl http://localhost:8080/api/inventory/items
```

2. Crear pedido:

```powershell
curl -X POST http://localhost:8080/api/orders `
  -H "Content-Type: application/json" `
  -d '{
    "customerName": "Ana Torres",
    "customerEmail": "ana@cliente.cl",
    "shippingAddress": "Av. Providencia 1234, Santiago",
    "lines": [
      { "sku": "SKU-1001", "quantity": 2, "unitPrice": 29990 },
      { "sku": "SKU-2001", "quantity": 1, "unitPrice": 14990 }
    ]
  }'
```

3. Consultar pedidos y envios:

```powershell
curl http://localhost:8080/api/orders
curl http://localhost:8080/api/shipments
```

## 13. Riesgos tecnicos actuales

- H2 en memoria no persiste datos entre reinicios.
- Comunicacion entre servicios es sincrona y puede aumentar latencia total.
- No hay trazabilidad distribuida para seguir una transaccion extremo a extremo.
- No hay seguridad de APIs (autenticacion/autorizacion) en esta version base.
- Fallback de envio evita caida total, pero puede dejar ordenes con gestion manual.

## 14. Propuesta de evolucion (Parcial 2/3)

### Prioridad alta

1. Seguridad
    - JWT en gateway y propagacion de identidad a servicios internos.
2. Observabilidad
    - Correlation ID, logs estructurados, trazas distribuidas (Micrometer + OpenTelemetry/Zipkin).
3. Persistencia productiva
    - Migrar H2 a PostgreSQL/MySQL por servicio con migraciones (Flyway/Liquibase).

### Prioridad media

4. Mensajeria asincrona
    - Eventos de dominio para pedidos y envios (Kafka/RabbitMQ).
5. Resiliencia avanzada
    - Timeouts, retries con backoff, bulkhead y politicas de idempotencia.

### Prioridad baja

6. Frontend de operacion
    - Panel para monitoreo de pedidos/envios e intervenciones manuales.
7. Despliegue cloud-native
    - Kubernetes + Helm + pipelines CI/CD.

## 15. Conclusiones

SmartLogix implementa una base solida de arquitectura de microservicios para el escenario logistico planteado: separacion por dominios, descubrimiento de servicios, gateway central, resiliencia en integracion critica y flujo funcional completo de pedido-inventario-envio. La plataforma queda preparada para evolucionar hacia una solucion productiva con seguridad, observabilidad y procesamiento asincrono.

## 16. Referencias internas del repositorio

- `README.md`
- `pom.xml`
- `docker-compose.yml`
- `run-services.ps1`
- `run-docker.ps1`
- `api-gateway/src/main/resources/application.yml`
- `discovery-service/src/main/resources/application.yml`
- `inventory-service/src/main/resources/application.yml`
- `order-service/src/main/resources/application.yml`
- `shipment-service/src/main/resources/application.yml`
- `inventory-service/src/main/java/com/smartlogix/inventory/controller/InventoryController.java`
- `order-service/src/main/java/com/smartlogix/order/controller/OrderController.java`
- `shipment-service/src/main/java/com/smartlogix/shipment/controller/ShipmentController.java`
- `order-service/src/main/java/com/smartlogix/order/service/OrderService.java`
- `order-service/src/main/java/com/smartlogix/order/client/ShipmentClient.java`
- `shipment-service/src/main/java/com/smartlogix/shipment/factory/ShipmentPlanFactoryResolver.java`
