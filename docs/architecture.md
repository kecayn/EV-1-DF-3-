# Arquitectura de SmartLogix - Diagrama de Microservicios

## Diagrama de arquitectura (Mermaid)

```mermaid
graph TB
    Client["Cliente (curl / Frontend)"]

    subgraph Docker Compose
        GW["api-gateway\n:8080"]
        DS["discovery-service\n(Eureka) :8761"]

        subgraph Logistica
            INV["inventory-service\n:8081"]
            ORD["order-service\n:8082"]
            SHP["shipment-service\n:8083"]
        end

        subgraph Identidad
            USR["users-service\n:8084"]
            AUTH["auth-service\n:8085"]
        end

        DB_USR[("H2 (usersdb)")]
        DB_INV[("H2 (inventorydb)")]
        DB_ORD[("H2 (orderdb)")]
        DB_SHP[("H2 (shipmentdb)")]
    end

    Client -->|HTTP| GW
    GW -->|/api/inventory/**| INV
    GW -->|/api/orders/**| ORD
    GW -->|/api/shipments/**| SHP
    GW -->|/api/users/**| USR
    GW -->|/api/auth/**| AUTH

    AUTH -->|WebClient lb://users-service\nPOST /api/users/internal/check-credentials| USR

    INV --- DB_INV
    ORD --- DB_ORD
    SHP --- DB_SHP
    USR --- DB_USR

    DS -. "registro Eureka" .- INV
    DS -. "registro Eureka" .- ORD
    DS -. "registro Eureka" .- SHP
    DS -. "registro Eureka" .- USR
    DS -. "registro Eureka" .- AUTH
    DS -. "registro Eureka" .- GW
```

## Flujo de autenticacion JWT

```mermaid
sequenceDiagram
    actor C as Cliente
    participant GW as api-gateway
    participant AUTH as auth-service
    participant USR as users-service

    C->>GW: POST /api/auth/login {username, password}
    GW->>AUTH: reenvio de solicitud
    AUTH->>USR: POST /api/users/internal/check-credentials {username, rawPassword}
    USR-->>AUTH: {valid: true, userId, username}
    AUTH-->>GW: {token: "eyJ...", tokenType: "Bearer", expiresIn: 3600}
    GW-->>C: 200 OK + TokenResponse

    C->>GW: POST /api/auth/validate {token}
    GW->>AUTH: reenvio
    AUTH-->>GW: {valid: true, username: "alice", message: "Token is valid"}
    GW-->>C: 200 OK + ValidateTokenResponse
```

## Patrones de diseno aplicados

| Patron | Servicio | Descripcion |
|--------|----------|-------------|
| Repository | users-service | `UserRepository` extiende `JpaRepository` encapsulando acceso a datos |
| Strategy | auth-service | `TokenStrategy` interfaz con implementacion `JwtTokenStrategy`; permite cambiar el mecanismo de tokens sin modificar `AuthService` |
| Factory Method | shipment-service | `ShipmentPlanFactoryResolver` crea planes de envio por zona |
| Circuit Breaker | order-service | Resilience4j protege llamadas a shipment-service |
| Service Discovery | todos | Eureka registra y localiza servicios dinamicamente |
| API Gateway | api-gateway | Punto unico de entrada; enruta por path |
