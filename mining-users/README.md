<br />
<div align="center">
<h3 align="center">Mining Project - USERS MICROSERVICE</h3>
  <p align="center">
    Microservicio de identidad y autenticación del middleware de alerta temprana de contaminación hídrica por minería. Maneja usuarios con roles, emisión de JWT y validaciones de permisos.
  </p>
</div>

### Built With

* ![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=java&logoColor=white)
* ![Spring](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
* ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=Spring-Security&logoColor=white)
* ![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)
* ![Gradle](https://img.shields.io/badge/Gradle-02303A.svg?style=for-the-badge&logo=Gradle&logoColor=white)
* ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)

## Descripción General

Este microservicio es el núcleo de identidad del middleware (ver el [README raíz](../README.md)). Es responsable de:

- **Gestión de usuarios**: Creación, consulta y listado de usuarios con validaciones de negocio
- **Autenticación**: Login con JWT (JSON Web Tokens) para sesiones seguras
- **Autorización**: Validación de roles y permisos jerárquicos
- **Validación de identidad**: Verificación de documentos únicos, emails válidos y mayoría de edad
- **Roles soportados**:
  - `ADMINISTRADOR`: administra la plataforma, consulta la auditoría y crea usuarios INGENIERO
  - `INGENIERO`: registra adapters e ingiere mediciones (pasarelas de sensores y laboratorio); crea usuarios MODERADOR
  - `MODERADOR`: valida o rechaza reportes ciudadanos y ve su ubicación exacta
  - `CLIENTE`: ciudadano que se registra por `/users/public` y crea reportes

  La autorización de cada operación la aplica `mining-management` a partir del rol y el `userId` firmados en el token.

### Arquitectura

El proyecto sigue **Arquitectura Hexagonal (Puertos y Adaptadores)** con **API-First Design**:

```
src/
├── domain/              # Lógica de negocio pura
│   ├── model/          # Modelos de dominio
│   ├── usecase/        # Casos de uso
│   ├── api/            # Puertos de entrada (interfaces)
│   └── spi/            # Puertos de salida (interfaces)
├── application/         # Capa de aplicación
│   ├── handler/        # Handlers/Facades
│   └── mapper/         # Mappers (MapStruct)
└── infrastructure/      # Adaptadores
    ├── input/rest/     # Controladores REST
    ├── out/jpa/        # Adaptadores de persistencia
    ├── out/security/   # Adaptadores de seguridad
    └── security/       # Configuración de seguridad
```


### Microservicio Principal

[`mining-management`](../mining-management/README.md)


## Endpoints Implementados

### Autenticación

#### `POST /auth/login`
Iniciar sesión y obtener token JWT.

**Request Body:**
```json
{
  "email": "admin@admin.com",
  "password": "admin123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "role": "ADMINISTRADOR",
  "email": "admin@admin.com"
}
```

---

### Usuarios

#### `POST /users`
Crear un nuevo usuario (rol depende del usuario autenticado).

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "name": "Juan",
  "lastName": "Pérez",
  "password": "MiClaveSegura123",
  "email": "juanpere07@email.com",
  "identificationDocument": "1348917990",
  "phoneNumber": "+57j3005698325",
  "birthDate": "2000-01-01",
  "roleId": 2
}
```

**Validaciones:**
- ADMINISTRADOR → puede crear INGENIERO (roleId=2)
- INGENIERO → puede crear MODERADOR (roleId=3)
- Email y documento únicos
- Usuario mayor de 18 años
- Teléfono máximo 13 caracteres, puede incluir +
- Password encriptado con BCrypt

**Response (201 Created):**
```json
{
  "data": {
    "id": 5,
    "name": "Juan",
    "lastName": "Pérez",
    "documentNumber": "1234567890",
    "phoneNumber": "+573001234567",
    "email": "juan.perez@example.com",
    "role": "INGENIERO"
  }
}
```

---

#### `GET /users`
Listar usuarios (paginado).

**Query Parameters:**
- `page`: Número de página (default: 0)
- `size`: Tamaño de página (default: 10)

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": 1,
      "name": "Admin",
      "lastName": "User",
      "email": "admin@admin.com",
      "role": "ADMINISTRADOR"
    }
  ],
  "meta": {
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

#### `GET /users/{id}`
Obtener usuario por ID.

**Response (200 OK):**
```json
{
  "data": {
    "id": 1,
    "name": "Admin",
    "lastName": "User",
    "documentNumber": "1234567890",
    "phoneNumber": "+573001234567",
    "email": "admin@admin.com",
    "role": "ADMINISTRADOR"
  }
}
```

---

#### `GET /users/public`
Registro publico, un CLIENTE puede registrarse sin autenticación.

**Response (200 OK):**
```json
{
  "valid": true,
  "userId": 5,
  "roleId": 2,
  "roleName": "INGENIERO"
}
```

---

<!-- USAGE -->
## Pruebas Unitarias

### Cobertura de Historias de Usuario

Este microservicio cubre **4 Historias de Usuario** con más de **21 pruebas unitarias**:

| Historia | Clase de Test | Pruebas |
|----------|---------------|---------|
| HU-1: Crear Propietario | `UserUseCaseTest` | ✅ Validación mayoría edad<br>✅ Validación email único<br>✅ Validación documento único |
| HU-5: Autenticación | `AuthenticationUseCaseTest` | ✅ Login exitoso<br>✅ Credenciales inválidas<br>✅ Usuario no existe<br>✅ Generación JWT |
| HU-6: Crear Empleado | `UserUseCaseTest` | ✅ Validación rol ingeniero<br>✅ Creación exitosa moderador |
| HU-8: Crear Cliente | `UserUseCaseTest` | ✅ Creación cuenta cliente<br>✅ Validaciones generales |

### Ejecutar Tests

```bash
# Todos los tests con cobertura
./gradlew test jacocoTestReport

# Ver reporte HTML
start build/reports/tests/test/index.html
start build/reports/jacoco/test/html/index.html

# Tests específicos
./gradlew test --tests "UserUseCaseTest"
./gradlew test --tests "AuthenticationUseCaseTest"
```

### Tecnologías de Testing

- **JUnit 5**: Framework de pruebas
- **Mockito**: Mocking de dependencias
- **JaCoCo**: Reporte de cobertura de código
- **AssertJ**: Aserciones fluidas

---

## Cómo Ejecutar Localmente

### 1. Instalación

1. **Clonar el repositorio**
   ```bash
   git clone <repository-url>
   cd mining-project/mining-users
   ```

2. **Crear base de datos en PostgreSQL**
   ```sql
   CREATE DATABASE users;
   ```

3. **Configurar conexión a base de datos**
   
   Editar `src/main/resources/application-dev.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/users
       username: postgres
       password: tu_contraseña
   ```

4. **Configurar variables de entorno o directamente en tu archivo application.yml**
   ```bash
   # JWT Configuration
   JWT_SECRET=your_secret_key_here
   JWT_EXPIRATION=86400000
   ```

### 2. Compilar el Proyecto

```bash
# Generar DTOs desde OpenAPI spec
./gradlew openApiGenerate

# Compilar todo el proyecto
./gradlew clean build
```

### 3. Ejecutar la Aplicación

**Opción 1: Desde terminal**
```bash
./gradlew bootRun
```

**Opción 2: Desde IntelliJ IDEA**
- Right-click `PowerUpApplication.java` → Run


### 4. Datos Iniciales

- Ejecutar el archivo **init-roles.sql** en tu base de datos para crear los roles iniciales y autoinsertar el usuario administrador
```sql
INSERT INTO roles (id, name, description) VALUES
                                              (1, 'ADMINISTRADOR', 'Administrador de la plataforma'),
                                              (2, 'INGENIERO', 'Ingeniero o técnico ambiental (sensores y adaptadores)'),
                                              (3, 'MODERADOR', 'Validador de reportes ciudadanos'),
                                              (4, 'CLIENTE', 'Ciudadano que reporta')
    ON CONFLICT (name) DO NOTHING;

-- Crear usuario administrador inicial
INSERT INTO users (
    id, name, last_name, password, email, identification_document, phone_number, birth_date, role_id, created_at
) VALUES (
             1,
             'Admin',
             'Sistema',
             '$2a$10$p4a7VE8q0enYsvh2WMn1mO.LulXkflkDZSbHLVX.Lr8T5lW3EmfB6',
             'admin@admin.com',
             '1234567890',
             '+573001234567',
             '1990-01-01',
             1,
             NOW()
         ) ON CONFLICT (email) DO NOTHING;

SELECT setval('roles_id_seq', 4, true);
```

### 5. Credenciales de Acceso luego de ejecutar el script SQL
- Email: admin@admin.com
- Contraseña: Admin123

Solo para uso local: cambie esta contraseña y el secreto JWT antes de cualquier despliegue real.

---

### Seguridad

#### JWT (JSON Web Tokens)
El microservicio usa JWT para autenticación y autorización:

**Claims en el token:**
- `userId`: ID del usuario autenticado
- `role`: Rol del usuario
- `roleId`: ID numérico del rol
- `email`: Email del usuario

**Flujo de autenticación:**
1. Usuario hace login con email/password
2. Sistema valida credenciales
3. Sistema genera token JWT
4. Cliente incluye token en header: `Authorization: Bearer <token>`
5. Sistema valida token en cada request

---

## Autor

**Brayan Alexander Barco Cardenas** — [ORCID 0009-0006-0332-7625](https://orcid.org/0009-0006-0332-7625)





