# Sistema de Gestión de Minería API
Plataforma basada en microservicios desarrollada con **Spring Boot (Java 17)** y estructurada bajo **Arquitectura Hexagonal**. Permite el registro de actividad minera ilegal, datos de sensores ambientales y notificaciones distribuidas.
---
## ??? Arquitectura de Microservicios
El sistema está diseñado en 3 microservicios principales:
1. **mining-management (Core / Puerto 8080)**: 
   - Microservicio principal y orquestador.
   - Gestión de reportes de denuncias ciudadanas y foros.
   - Orquestador lógico (API Gateway Proxy) para consumir el microservicio de autenticación.
   - Mapping, serialización y almacenamiento dinámico de datos de sensores ambientales y cálculo de agregados.
   - Almacenamiento seguro por S3.
   - *Actúa como Emisor de eventos asíncronos en Kafka.*
2. **mining-users (Auth / Puerto 8081)**:
   - Microservicio dedicado puramente a Identidad y Acceso (IAM).
   - Emisión de JWT y gestión de control de roles (RBAC).
3. **mining-audit-notifications (Event-Driven / Puerto 8082)**:
   - Consumidor puro orientado a eventos.
   - Recibe la carga de crear Notificaciones mediante MongoDb.
   - Recibe la carga de generar trazabilidad de auditoría inmutable (Append-only) utilizando PostgreSQL.
---
## ?? Despliegue y Base de Datos
Todos los servicios de soporte han sido configurados a través de **Docker Compose**.
En la raíz del proyecto, ejecuta:
\\\ash
docker-compose up -d
\\\
Esto levantará los siguientes servicios en segundo plano:
- **Zookeeper & Kafka**: Infraestructura asíncrona en el puerto \9092\.
- **PostgreSQL**: Base de datos SQL para auditoría en el puerto \5432\.
- **MongoDB**: Base de datos NoSQL para notificaciones en el puerto \27017\.
- **Prometheus**: Base de datos Time-Series en el puerto \9090\.
- **Grafana**: Dashboard de visualización en el puerto \3000\.
---
## ?? Observabilidad (Prometheus & Grafana)
La plataforma completa ha sido instrumentada con **Micrometer** y **Spring Boot Actuator** en los 3 microservicios generados.
- **Prometheus** está pre-configurado (\observability/prometheus.yml\) para interrogar localmente a los 3 microservicios cada 15 segundos sobre sus métricas \/actuator/prometheus\.
- **Grafana** permite visualizar el comportamiento interno de la JVM y rendimiento HTTP. 
  - *Acceso Grafana:* [http://localhost:3000](http://localhost:3000)
  - *Credenciales por defecto:* \dmin\ / \dmin\
  - *Setup:* La base de datos \Prometheus\ ya fue añadida como proveedor de forma autoconfigurada por *Provisioning*. Puedes agregar dashboards como el **JVM Micrometer**.
---
## ?? Referencia Rápida de Endpoints
### 1. Autenticación (Vía Core)
- \POST /api/v1/auth/login\ - Proxifica hacia \mining-users\ devolviendo la sesión JWT.
### 2. CORE: Reportes & Foro
- \POST /api/v1/posts\ - Registro (REPORT/FORUM).
- \GET /api/v1/posts\ - Paginado.
- \GET /api/v1/reports\ - Múltiples Query Params dinámicos para los reportes especiales de minería ilegal.
- Flujos de estado: \PATCH /api/v1/reports/{id}/request-validation\ y \PATCH /api/v1/reports/{id}/status\.
### 3. CORE: Environmental Data
- \POST /api/v1/adapters\ - Creación de plantilla/mapping JSON.
- \POST /api/v1/environmental-data\ - Inserción cruda de sensores.
- \GET /api/v1/environmental-data/aggregate\ - Cálculo de parámetros estadísticos.
### 4. CORE: Storage AWS
- \POST /api/storage/upload\ - Sube archivos normales con MultipartFile.
- \POST /api/storage/upload-with-thumbnail\ - Generación automática de miniaturas.
### 5. EVENT-DRIVEN: Audit y Notification
- \GET /api/v1/notifications/{userId}\ - Bandejas de Notificaciones universales.
- \GET /api/v1/audit\ - Consulta de Logs inmutables de auditorías de todo el sistema.
---
## ??? Herramientas de Desarrollo
### Postman Collection Completa
Se incluye a nivel raíz el archivo **\postman_collection.json\**. Incluye todas las cabeceras, rutas con las llamadas asíncronas, simulación de autenticación proxy JWT y casos de uso del Core a los puertos definidos.
