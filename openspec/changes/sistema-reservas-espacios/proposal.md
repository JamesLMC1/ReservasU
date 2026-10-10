# Propuesta

## Por qué

La universidad necesita un sistema centralizado para reservar laboratorios, auditorios y salas de cómputo. Hoy no existe un mecanismo común que impida reservas solapadas, que aplique permisos por rol ni que deje trazabilidad de las decisiones. Este cambio define el comportamiento base del sistema, según `PLAN_INGENIERIA.md`, antes de empezar la implementación.

## Qué cambia

- Gestión de usuarios con tres roles: **administrador**, **aprobador** y **solicitante**. Solo el administrador crea usuarios y asigna roles.
- Gestión de espacios académicos de tres tipos: **laboratorio**, **auditorio** y **sala de cómputo**.
- Creación y consulta de solicitudes de reserva con validación de disponibilidad. Dos reservas activas no pueden solaparse en el mismo espacio, ni siquiera ante solicitudes simultáneas.
- Regla de negocio: **ninguna reserva puede durar más de cuatro horas consecutivas**. Se aplica a cada reserva individual (decisión tomada en el plan; ver la pregunta abierta en `design.md`).
- Aprobación y rechazo de solicitudes pendientes por parte de aprobadores o administradores. El rechazo exige motivo.
- Cancelación de reservas por el solicitante propietario, o por aprobador o administrador.
- Historial de cambios de estado de cada reserva.
- Control de permisos por rol en el backend para cada operación.

Fuera de alcance en esta versión: reservas recurrentes, notificaciones por correo, límites de capacidad de los espacios, integración con un directorio institucional y la modificación de la regla de cuatro horas, que será un cambio posterior (Fase 5).

## Capacidades

### Nuevas capacidades

- `usuarios-y-roles`: registro y gestión de usuarios, asignación de roles y matriz de permisos por rol.
- `espacios-academicos`: alta, modificación y desactivación de laboratorios, auditorios y salas de cómputo.
- `reservas`: creación, consulta y cancelación de reservas, validación de disponibilidad, regla de duración máxima de cuatro horas e historial de cambios de estado.
- `aprobacion-reservas`: aprobación y rechazo de solicitudes pendientes, con motivo obligatorio en el rechazo.

### Capacidades modificadas

Ninguna. El repositorio no tiene especificaciones previas en `openspec/specs/`.

## Impacto

- **Backend:** Java 21, Spring Boot 3 y Maven. Organización en controladores, servicios, repositorios y entidades. Seguridad con Spring Security y JWT.
- **Base de datos:** PostgreSQL con migraciones Flyway. Restricción de exclusión sobre intervalos para impedir solapamientos.
- **Frontend:** React con TypeScript. Solo muestra acciones permitidas según el rol; la autorización real está en el backend.
- **Pruebas:** JUnit 5 y Mockito para servicios; pruebas de integración y de concurrencia contra PostgreSQL.
- **Dependencias:** las versiones exactas se fijarán en Fase 4.
