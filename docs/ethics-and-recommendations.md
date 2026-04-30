# Recomendaciones Eticas y Tecnicas - SmartLogix

Este documento presenta recomendaciones eticas y tecnicas derivadas del analisis de la arquitectura de autenticacion y gestion de usuarios implementada en SmartLogix.

---

## 1. Privacidad y proteccion de datos

**Observacion**: Los usuarios confian sus credenciales al sistema. Una mala practica en el almacenamiento o transmision de contrasenas puede comprometer su privacidad irreversiblemente.

**Recomendaciones**:

- **No almacenar contrasenas en texto plano**. Este proyecto utiliza BCrypt (factor de costo 10 por defecto) para hashear contrasenas antes de persistirlas. BCrypt incluye un salt aleatorio por contrasena, lo que protege contra ataques de rainbow table.
- **No registrar contrasenas en logs**. Los logs de `AuthService` y `UserService` incluyen unicamente `username` y tiempos de respuesta; nunca el password raw.
- **Cumplir con principios de minima recopilacion**: la API de usuarios solo solicita `username`, `email` y `password`. No se recogen datos adicionales innecesarios.
- **En produccion**: habilitar HTTPS (TLS) en el API Gateway para cifrar la transmision de credenciales. Actualmente el sistema esta preparado para agregarlo a nivel de proxy inverso (Nginx/Traefik) sin modificar el codigo de los servicios.

---

## 2. Sostenibilidad y uso eficiente de recursos

**Observacion**: Los microservicios mal dimensionados pueden desperdiciar recursos de computo y energia, especialmente en entornos cloud con cobro por uso.

**Recomendaciones**:

- **Usar bases de datos en memoria (H2) solo en desarrollo**. Para produccion, migrar a PostgreSQL o MySQL compartido por servicio o usar instancias gestionadas que permitan escalado controlado.
- **Configurar limites de memoria JVM** en los Dockerfiles de produccion (`-Xmx256m -Xms128m`) para evitar sobreconsumo en contenedores con poca carga.
- **Habilitar Spring Boot Actuator con metricas Micrometer** (ya configurado con `/actuator/health`). Extender con `micrometer-registry-prometheus` para recopilar metricas de CPU, heap y tiempos de solicitud, permitiendo ajuste reactivo de replicas.
- **Tokens JWT de corta vida** (1 hora por defecto en este proyecto) reducen la necesidad de validaciones frecuentes y permiten revocar sesiones rapidamente sin almacenamiento de estado.

---

## 3. Accesibilidad, usabilidad y calidad de la API

**Observacion**: Una API con errores poco descriptivos o codigos HTTP incorrectos dificulta la integracion y genera frustracion en desarrolladores y usuarios finales.

**Recomendaciones**:

- **Usar codigos HTTP semanticamente correctos**: este proyecto responde `201 Created` al crear usuario, `401 Unauthorized` para credenciales invalidas, `409 Conflict` para usuarios duplicados y `404 Not Found` para usuarios inexistentes. Esto permite que clientes interpreten errores automaticamente.
- **Mensajes de error estructurados**: el `GlobalExceptionHandler` de ambos servicios retorna JSON con `timestamp`, `status`, `error` y `message`, facilitando la depuracion sin exponer detalles internos del sistema.
- **Validacion de entradas en el servidor**: se usan anotaciones `@NotBlank`, `@Email`, `@Size` para rechazar datos invalidos con mensajes claros antes de procesarlos, reduciendo errores silenciosos.
- **Documentar la API con OpenAPI/Swagger** (recomendado para produccion): agregar `springdoc-openapi-starter-webmvc-ui` permitiria generar documentacion interactiva en `/swagger-ui.html`, mejorando la accesibilidad para equipos de integracion.

---

## 4. Seguridad adicional (recomendacion tecnica)

- **Rotar el secreto JWT** mediante variables de entorno (`JWT_SECRET`) en lugar de valores hardcoded. En produccion, usar un gestor de secretos (AWS Secrets Manager, Vault).
- **Rate limiting** en el endpoint `/api/auth/login` para prevenir ataques de fuerza bruta (implementable con Spring Cloud Gateway + Redis).
- **Agregar refresh tokens** para permitir sesiones de larga duracion sin comprometer la seguridad de los access tokens.
