# Mining Audit Notifications API

Api para manejar notificaciones y auditoria de forma asincrona con kafka
Uso de NoSql (mongoDB) para manejar las notificaciones
Uso de sql (postgres) para manejar la auditoria

---

## 🏗️ Arquitectura Hexagonal

El sistema está dividido en capas con estrictos límites de dependencia:

- **Domain:** Modelos de negocio puros (`AuditLog`, `Notification`, `EnvironmentalAlert`).
- **Application:** Contiene los flujos principales (casos de uso) y define los `Ports` de entrada (API) y salida (SPI).
- **Infrastructure:**
  - **Input (Adapters IN):** RestControllers manejando las peticiones HTTP y mapeando `DTOs` (usando `MapStruct`).
  - **Output (Adapters OUT):** Orquestación con persistencia vía **Spring Data JPA**.

---

## 🚀 Funcionalidades

### 1. Sistema de Eventos (Kafka)
- **Consumer Desacoplado:** Suscrito al tópico `mining-events` para reaccionar a eventos del core de manera asíncrona (ej. `REPORT_CREATED`, `REPORT_VALIDATED`, `COMMENT_CREATED`).
- **Resiliencia (Retry & DLT):** Se configuran reintentos (`@RetryableTopic`) automáticos y "Dead Letter Queues" para mensajes que fallen, asegurando cero pérdida de datos.

- **Alertas ambientales:** Suscrito al tópico `environmental-alerts`. Cada alerta (ALERTA o CRÍTICO) se procesa una sola vez aunque Kafka la entregue de nuevo (deduplicación por identificador de medición), se registra en la auditoría y se notifica a los destinatarios configurados (`ALERT_RECIPIENTS`). Métricas: `alert.e2e.latency` (ingesta → notificación persistida), `alert.broker.latency` y `alert.duplicates.discarded`, expuestas en `/actuator/prometheus`.

### 2. Sistema de Notificaciones (MongoDB)
- Persistencia de alto rendimiento utilizando bases de datos documentales.
- **Endpoints:**
  - `GET /api/v1/notifications/{userId}`: Consulta universal de notificaciones de un usuario.
  - `GET /api/v1/notifications/{userId}/unread`: Consulta solo notificaciones no leídas.
  - `PATCH /api/v1/notifications/{id}/read`: Marca una notificación específica como leída.

### 3. Registro de Auditoría (PostgreSQL)
- Trazabilidad inmutable (Append-only) de qué ocurrió, quién lo hizo y los metadatos exactos de la acción a través del tiempo.
- **Endpoints:**
  - `GET /api/v1/audit`: Consulta dinámica con filtros avanzados (por usuario, acción, y rango de fechas ISO).
