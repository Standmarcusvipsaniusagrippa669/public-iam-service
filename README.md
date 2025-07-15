# Valiantech IAM Core API

## Introducción

Valiantech IAM Core API es un sistema modular de gestión de identidad y acceso (IAM) diseñado para pequeñas y medianas empresas (PYMEs).  
Ofrece funcionalidades robustas de autenticación, autorización, gestión de usuarios y empresas, auditoría y seguridad, con un enfoque en eficiencia y escalabilidad.

---

## Funcionalidades Clave

- Autenticación segura con JWT y refresh tokens  
- Login en dos pasos y control de sesión  
- Gestión completa de usuarios, empresas y roles  
- Flujo de invitaciones para incorporación de usuarios  
- Auditoría detallada y registro de eventos críticos  
- Integración configurable con servicios de correo y proveedores externos  
- Configuración flexible para ambientes `local`, `develop`, `qa`, `prod`  

---

## Tecnologías

- Java 17, Spring Boot 3  
- PostgreSQL con Flyway para migraciones  
- Redis para manejo de sesiones y tokens  
- Docker para contenedorización y despliegue  
- Integración con Mailtrap y SendGrid para envío de emails  

---

## Requisitos Previos

- Docker y Docker Compose instalados  
- Acceso a base de datos PostgreSQL y Redis (local o en la nube)  
- Variables de entorno configuradas (ver sección Configuración)  

---

## Configuración

Define las siguientes variables de entorno en tu entorno Docker o sistema:

```env
DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/iamdb
DATASOURCE_USERNAME=iamuser
DATASOURCE_PASSWORD=secretpassword

JWT_SECRET_VALUE=tu_secreto_jwt
SPRING_PROFILES_ACTIVE=docker

REDIS_HOST=host.docker.internal
REDIS_PORT=6379
REDIS_PASSWORD=redispass
REDIS_IS_CLUSTER=false

RATE_LIMIT_WHITELIST=10.0.0.1,200.1.2.3
INVITATION_REGISTRATION_URL_BASE=https://auth.techvalian.com/invitation
INVITATION_TOKEN_EXPIRY_DAYS=1

LOGGING_SENSITIVE_FIELDS=currentPassword,newPassword,password

SMTP_HOST=sandbox.smtp.mailtrap.io
SMTP_USERNAME=tu_usuario_mailtrap
SMTP_PASSWORD=tu_password_mailtrap
```

---

## Despliegue con Docker

### 1. Construir la imagen Docker

```bash
./mvnw clean package -DskipTests
docker build -t valiantech-iam-core .
```

### 2. Ejecutar contenedores necesarios (PostgreSQL y Redis)

Si no tienes servicios externos, puedes usar Docker Compose:

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: iamdb
      POSTGRES_USER: iamuser
      POSTGRES_PASSWORD: secretpassword
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7
    command: ["redis-server", "--requirepass", "redispass"]
    ports:
      - "6379:6379"

volumes:
  pgdata:
```

Ejecuta:

```bash
docker-compose up -d
```

### 3. Ejecutar la API

```bash
docker run --rm \
  -e DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/iamdb \
  -e DATASOURCE_USERNAME=iamuser \
  -e DATASOURCE_PASSWORD=secretpassword \
  -e JWT_SECRET_VALUE=tu_secreto_jwt \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  -e REDIS_PASSWORD=redispass \
  -e LOGGING_SENSITIVE_FIELDS=currentPassword,newPassword,password \
  -e SMTP_HOST=sandbox.smtp.mailtrap.io \
  -e SMTP_USERNAME=tu_usuario_mailtrap \
  -e SMTP_PASSWORD=tu_password_mailtrap \
  -p 8080:8080 \
  valiantech-iam-core
```

### 4. Accede a la API

Abre [http://localhost:8080/api/v1/](http://localhost:8080/api/v1/) y prueba los endpoints.

---

## Documentación Adicional

- Documentación técnica detallada (arquitectura, contribuciones) se encuentra en carpetas separadas.  
- Para desarrollo local usa perfil `local` y configuración específica.  
- Para producción considera variables de entorno y seguridad reforzada.

---

## Contacto

Para dudas o contribuciones contacta a [iancardenasc@valiantech.com](mailto:iancardenasc@valiantech.com).

---

**© 2025 Valian Technologies SpA. Todos los derechos reservados.**

Este documento forma parte de un sistema informático de propiedad exclusiva de Valian Technologies SpA, empresa constituida en Chile.  
Su uso, copia, distribución o reproducción total o parcial, para cualquier fin (incluyendo fines comerciales, educativos o de cualquier otra índole), está prohibido sin autorización expresa y por escrito de Valian Technologies SpA.

---