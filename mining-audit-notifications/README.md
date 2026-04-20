# Mining Management API

API Backend robusta desarrollada con **Spring Boot (Java 17)** estructurada bajo los principios de **Arquitectura Hexagonal (Ports and Adapters)** para el registro y gestión de actividad minera ilegal y datos de sensores ambientales.

---

## 🏗️ Arquitectura Hexagonal

El sistema está dividido en capas con estrictos límites de dependencia:

- **Domain:** Modelos de negocio puros (`Post`, `Report`, `EnvironmentalData`, `Adapter`, `Comment`). Lógica interna de los modelos (e.g. validaciones de estado de denuncias).
- **Application:** Contiene los flujos principales (casos de uso) y define los `Ports` de entrada (API) y salida (SPI).
- **Infrastructure:**
  - **Input (Adapters IN):** RestControllers manejando las peticiones HTTP y mapeando `DTOs` (usando `MapStruct`).
  - **Output (Adapters OUT):** Orquestación con persistencia vía **Spring Data JPA**.

---

## 🚀 Funcionalidades

### 1. Gestión de Reportes y Posts (Foro)
- **Creación de Denuncias:** Publicación de `REPORT` (obligatorio incluir Latitud y Longitud) o `FORUM`.
- **Validación de Flujos:** El estado base de los reportes es `SIN_VALIDAR`. Solo reportes en status `PENDIENTE` pueden ser validados (Aceptados o Rechazados) mediante lógica atómica de entidad en los endpoins de `PATCH`.
- **Paginación & Filtrado:** Acceso a listado `getAllPosts()` aplicando `Pageable`. Los reportes permiten filtrados dinámicos por **estado, fechas y delimitadores geográficos**.
- **Comentarios y Multimedia:** Relaciones `OneToMany` para adjuntar `PostImage` (URL) y generar interacción a través de comentarios.

### 2. Gestión de Sensores y Datos Ambientales
- **Formatos Dinámicos:** Endpoints dedicados al registro de adaptadores (Mappings). Permite a los dispositivos de sensores externos enviar la telemetría en el formato que sea.
- **Normalización (Mapping Engine):** Evaluador que convierte métricas entrantes por medio del mapeo JSON como `phwater_ong -> ph` ó JSON Path expressions (`meta.id -> device_id`).
- **Análisis Exploratorio:** Permite acceder a los datos usando *Criteria API* dinámicos basados en combinaciones múltiples (por ejemplo, buscar el `pH` mínimo, máximas temperaturas y ubicación determinada en el mismo query).
- **Cálculos Agrupados (Dashboards):** Servicio de cálculo agregado (Aggregations JPQL) de promedios, valores mínimos y máximos en un solo endpoint. 

---

## 🔌 Referencia de Endpoints

### Posts & Foros (`/api/v1/posts`)
- `POST /api/v1/posts` - Crea un foro o reporte dependiendo del `type` en el body.
- `GET /api/v1/posts` - Listado paginado universal.
- `GET /api/v1/posts/{postId}/comments` - Obtiene todos los comentarios de un post.

### Reportes (Denuncias) (`/api/v1/reports`)
- `GET /api/v1/reports` - Devuelve reportes (Soporta múltiples `Query Params` para filtrado).
- `GET /api/v1/reports/{id}` - Detalle en particular.
- `PATCH /api/v1/reports/{id}/request-validation` - Inicia flujo de revisión (pasa de `SIN_VALIDAR` a `PENDIENTE`).
- `PATCH /api/v1/reports/{id}/status` - Autoriza estado final (`VALIDADO`, `RECHAZADO`).

### Comentarios (`/api/v1/comments`)
- `POST /api/v1/comments` - Crea un registro anidado a un `Post`.

### Sensores Ambientales (`/api/v1/environmental-data` & `/adapters`)
- `POST /api/v1/adapters` - Registra la plantilla en formato JSON para un `sensorType`.
- `POST /api/v1/environmental-data` - Recepción del payload crudo a traducir.
- `GET /api/v1/environmental-data` - Búsqueda de registros ambientales por limitadores dinámicos.
- `GET /api/v1/environmental-data/aggregate` - Computa Min, Max, y Avg de variables climáticas consultadas.

---

## 🛠️ Herramientas para Entorno de Desarrollo

Se han configurado herramientas de integración para la fácil lectura y testing:

### 1. Swagger UI Automático
No es necesario leer código para conocer los formatos de entrada exacta, accede al servidor en ejecución y vé a:

> **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

*Detecta las firmas automáticamente (OpenAPI) sin polución en tu código de negocio.*

### 2. Postman Collection
Se importó y configuró todo el historial de uso en una Collection Postman automatizada:
- Localiza el archivo base: `postman_collection.json` e instálalo en Postman *(File -> Import).*
- Allí verás carpetas delimitadas con request precargados para testear simulaciones al instante.

---

## 🔧 Componentes y Librerías Principales
- **Spring Boot 3.x (Web, Data JPA, Validation)**
- **MapStruct & Lombok** (Eliminación de boilerplate y conversion DTO <-> Entity fluida)
- **PostgreSQL** para base de datos
- **Springdoc OpenAPI 2** (Generación Auto Swagger)
- **Manejo Estricto de Excepciones:** `ControllerAdvisor` restringe respuestas no mapeadas y blinda tu API de ataques.
