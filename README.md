# 🔐 ValianTech IAM Core API

**ValianTech IAM Core API** es un sistema modular de **gestión de identidad y acceso (IAM)** diseñado para PYMEs, startups y entornos SaaS multi-tenant.  
Ofrece un backend seguro, escalable y extensible para autenticación, autorización y administración de usuarios y empresas.

---

## 🚀 Características principales

- 🔑 Autenticación basada en **JWT + Refresh Tokens**
- 🔒 Login en dos pasos y control de sesiones activas
- 🧩 Gestión completa de **usuarios, roles y empresas**
- ✉️ Flujo de invitaciones para incorporación de usuarios
- 🧠 Auditoría detallada y registro de eventos críticos
- ⚙️ Integración configurable con proveedores externos (Mailtrap, SendGrid, etc.)
- 🌎 Perfiles de ejecución: `local`, `develop`, `qa`, `prod`

---

## 🧱 Tecnologías

- **Java 17**, **Spring Boot 3**
- **PostgreSQL** + **Flyway** (migraciones)
- **Redis** (sesiones y tokens)
- **Docker** (contenedorización y despliegue)
- **Gradle** o **Maven** (compilación flexible)

---

## ⚙️ Requisitos previos

- Docker y Docker Compose instalados
- Acceso a PostgreSQL y Redis (local o remoto)
- Variables de entorno configuradas correctamente (ver abajo)

---

## 🧩 Configuración

Crea un archivo `.env` o define las variables de entorno equivalentes:

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
INVITATION_REGISTRATION_URL_BASE=https://yourdomain.com/invitation
INVITATION_TOKEN_EXPIRY_DAYS=1

LOGGING_SENSITIVE_FIELDS=currentPassword,newPassword,password
```

---

## 🐳 Despliegue con Docker

### 1️⃣ Construir la imagen

**Con Maven (JVM build):**
```bash
./mvnw clean package -DskipTests
docker build -t valiantech-iam-core .
```

**Con Gradle (bootBuildImage):**
```bash
./gradlew clean bootBuildImage
```

---

### 2️⃣ Levantar dependencias locales

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
docker compose up -d
```

---

### 3️⃣ Ejecutar la API

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
  -p 8080:8080 \
  valiantech-iam-core
```

---

## 📄 Licencia

Este proyecto está bajo la licencia **MIT**, lo que permite su uso, modificación y distribución con fines personales o comerciales, siempre que se mantenga el aviso de copyright.

```text
MIT License

Copyright (c) 2025 Ian Cárdenas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🤝 Contribuciones

Las contribuciones son bienvenidas.  
Si deseas colaborar:

1. Haz un fork del repositorio.
2. Crea una rama (`feature/nueva-funcionalidad`).
3. Realiza tus cambios y abre un Pull Request.
4. Incluye una breve descripción y evidencias si aplica.

---

## 📬 Contacto

Para consultas o soporte: **[iancardenasc@techvalian.com](mailto:iancardenasc@techvalian.com)**  
