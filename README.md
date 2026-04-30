# SmartLogix - Plataforma Inteligente para Gestion Logistica (Microservicios)

Proyecto de referencia para el caso semestral de Informatica.
Incluye una arquitectura realista para PYMEs eCommerce con estos modulos:

- Gestion de Inventario (`inventory-service`)
- Procesamiento de Pedidos (`order-service`)
- Coordinacion de Envios (`shipment-service`)
- **Gestion de Usuarios (`users-service`)** — nuevo
- **Autenticacion JWT (`auth-service`)** — nuevo

Y componentes de infraestructura:

- Descubrimiento de servicios (`discovery-service` con Eureka)
- API Gateway (`api-gateway`)

## Patrones de arquitectura implementados

- `Service Discovery`: registro dinamico con Eureka.
- `API Gateway`: punto unico de entrada para frontend o clientes.
- `Database per Service`: cada microservicio usa su propia base H2.
- `Factory Method`: en `shipment-service` para crear planes de envio por zona.
- `Circuit Breaker`: en `order-service` para llamadas a `shipment-service`.
- `Synchronous orchestration`: `order-service` coordina inventario + envio.
- **`Repository Pattern`**: en `users-service` — `UserRepository` encapsula toda la persistencia de usuarios.
- **`Strategy Pattern`**: en `auth-service` — `TokenStrategy` permite intercambiar el mecanismo de tokens (JWT u otro) sin modificar la logica de autenticacion.

## Estructura del repositorio

- `discovery-service` (puerto `8761`)
- `api-gateway` (puerto `8080`)
- `inventory-service` (puerto `8081`)
- `order-service` (puerto `8082`)
- `shipment-service` (puerto `8083`)
- `users-service` (puerto `8084`) — nuevo
- `auth-service` (puerto `8085`) — nuevo

## Requisitos

- Java 17
- Maven Wrapper (`mvnw.cmd` ya incluido)

## Compilar y validar

```powershell
.\mvnw.cmd clean test
```

## Docker

Todas las imagenes de los microservicios usan multi-stage build con Java 17:

```dockerfile
FROM eclipse-temurin:17-jdk AS build
```

Para levantar toda la plataforma con Docker Compose:

```powershell
docker compose up --build -d
docker compose ps
```

Para detenerla:

```powershell
docker compose down
```

Si Docker Desktop no esta ejecutandose, `docker compose` devolvera un error de conexion al daemon.

Tambien puedes usar:

```powershell
.\run-docker.ps1
```

## Ejecutar (opcion 1: manual)

Iniciar en este orden (cada comando en terminal distinta):

```powershell
.\mvnw.cmd -pl discovery-service spring-boot:run
.\mvnw.cmd -pl inventory-service spring-boot:run
.\mvnw.cmd -pl shipment-service spring-boot:run
.\mvnw.cmd -pl order-service spring-boot:run
.\mvnw.cmd -pl users-service spring-boot:run
.\mvnw.cmd -pl auth-service spring-boot:run
.\mvnw.cmd -pl api-gateway spring-boot:run
```

## Ejecutar (opcion 2: script)

```powershell
.\run-services.ps1
```

## URLs principales

- Eureka Dashboard: `http://localhost:8761`
- API Gateway: `http://localhost:8080`

## Pruebas rapidas por Gateway

### 1) Crear un usuario

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "email": "alice@example.com", "password": "secret123"}'
```

Respuesta esperada (`201 Created`):
```json
{"id": 1, "username": "alice", "email": "alice@example.com", "createdAt": "..."}
```

### 2) Autenticar (login) y obtener JWT

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```

Respuesta esperada (`200 OK`):
```json
{"token": "eyJhbGci...", "tokenType": "Bearer", "expiresIn": 3600}
```

### 3) Validar token

```bash
curl -X POST http://localhost:8080/api/auth/validate \
  -H "Content-Type: application/json" \
  -d '{"token": "eyJhbGci..."}'
```

Respuesta esperada:
```json
{"valid": true, "username": "alice", "message": "Token is valid"}
```

### 4) Listar usuarios

```bash
curl http://localhost:8080/api/users
```

### 5) Listar inventario inicial

```powershell
curl http://localhost:8080/api/inventory/items
```

### 6) Crear un pedido

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

### 7) Ver pedidos

```powershell
curl http://localhost:8080/api/orders
```

### 8) Ver envios

```powershell
curl http://localhost:8080/api/shipments
```

## Endpoints clave

### Users Service (`/api/users`)

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| `POST` | `/api/users` | Crear usuario (password se almacena hasheada con BCrypt) |
| `GET` | `/api/users` | Listar todos los usuarios |
| `GET` | `/api/users/{id}` | Obtener usuario por ID |
| `PUT` | `/api/users/{id}` | Actualizar usuario |
| `DELETE` | `/api/users/{id}` | Eliminar usuario |

> Las respuestas **nunca incluyen** el hash de la contrasena.

### Auth Service (`/api/auth`)

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| `POST` | `/api/auth/login` | Autenticar y obtener JWT |
| `POST` | `/api/auth/validate` | Validar un token JWT |

### Inventory Service

- `GET /api/inventory/items`
- `POST /api/inventory/items`
- `GET /api/inventory/items/{sku}`
- `GET /api/inventory/items/{sku}/availability?quantity=...`
- `PATCH|POST /api/inventory/items/{sku}/reserve?quantity=...`
- `PATCH|POST /api/inventory/items/{sku}/release?quantity=...`
- `PATCH|POST /api/inventory/items/{sku}/dispatch?quantity=...`

### Order Service

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderNumber}`

### Shipment Service

- `POST /api/shipments`
- `GET /api/shipments`
- `GET /api/shipments/{trackingCode}`
- `PATCH /api/shipments/{trackingCode}/status?value=IN_TRANSIT`

## Flujo funcional implementado

1. Se crea usuario en `users-service` con contrasena hasheada en BCrypt.
2. Usuario se autentica via `auth-service`, que valida credenciales llamando a `users-service`.
3. `auth-service` emite un JWT firmado (valido 1 hora).
4. Se crea orden en `order-service`.
5. `order-service` valida disponibilidad en `inventory-service`.
6. Si hay stock, reserva unidades en inventario.
7. Solicita planificacion de envio en `shipment-service`.
8. Devuelve orden con `trackingCode` y estado final.

## Documentacion adicional

- `docs/architecture.md` — Diagrama de arquitectura con Mermaid y flujo JWT
- `docs/ethics-and-recommendations.md` — Recomendaciones eticas y tecnicas
- `docs/INFORME-TECNICO.md` — Informe tecnico completo

