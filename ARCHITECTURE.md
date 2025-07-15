# Arquitectura del Sistema - Valiantech IAM Core API

## Visión General

El sistema está diseñado como una API REST modular basada en Spring Boot que provee servicios de gestión de identidad y acceso (IAM) para pequeñas y medianas empresas.  
Se enfoca en escalabilidad, seguridad y mantenibilidad.

---

## Componentes Principales

- **API REST:** Expuesta mediante controladores Spring, sigue buenas prácticas RESTful.  
- **Servicio de Autenticación:** Manejo de JWT, login en dos pasos, refresh tokens, revocación y control de sesión.  
- **Gestión de Usuarios y Empresas:** CRUD, roles y vinculación multiempresa.  
- **Auditoría y Logs:** Registro exhaustivo de eventos críticos para trazabilidad y seguridad.  
- **Integraciones externas:** Email (Mailtrap/SendGrid), futuros SSO.

---

## Arquitectura de Paquetes

- `auth`: Autenticación y autorización, gestión de tokens.  
- `user`: Entidades y lógica de usuarios.  
- `company`: Empresas y onboarding.  
- `usercompany`: Relación usuarios-empresas y roles.  
- `invitation`: Flujos de invitación y registro.  
- `audit`: Logs de auditoría y actividad.  
- `security`: Validaciones y utilidades de seguridad.

---

## Persistencia

- PostgreSQL como base de datos relacional.  
- Uso de JPA y Hibernate para ORM.  
- Flyway para migraciones controladas.

---

## Seguridad

- Autenticación JWT con refresh tokens y revocación.  
- Roles y permisos granulares.  
- Protección con rate limiting y listas blancas/negras.  
- Almacenamiento seguro de credenciales y tokens.

---

## Escalabilidad y Despliegue

- Despliegue en contenedores Docker para portabilidad.  
- Preparado para orquestación con Kubernetes.  
- Redis para almacenamiento distribuido de sesiones y tokens.  
- Configuración mediante perfiles para distintos ambientes.

---

## Diagramas (por implementar)

- Diagrama de arquitectura general.  
- Diagrama de flujo de autenticación.  
- Diagrama entidad-relación para base de datos.

---

**© 2025 Valian Technologies SpA. Todos los derechos reservados.**

Este documento forma parte de un sistema informático de propiedad exclusiva de Valian Technologies SpA, empresa constituida en Chile.  
Su uso, copia, distribución o reproducción total o parcial, para cualquier fin (incluyendo fines comerciales, educativos o de cualquier otra índole), está prohibido sin autorización expresa y por escrito de Valian Technologies SpA.

---
