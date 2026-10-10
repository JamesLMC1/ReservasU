# Diseño

## Contexto

El repositorio no tiene código ni especificaciones previas. El motivo y el alcance están en `proposal.md`, y los requisitos exigibles en `specs/`. La arquitectura general (monolito modular, API REST, SPA React, PostgreSQL) está justificada en `PLAN_INGENIERIA.md`. Este documento fija las decisiones técnicas que las especificaciones no fijan.

## Objetivos / No objetivos

**Objetivos:**
- Garantizar en la base de datos que no existan dos reservas activas solapadas en el mismo espacio, también ante concurrencia.
- Aplicar la autorización por rol en el backend para cada operación.
- Mantener la separación en controladores, servicios, repositorios y entidades.

**No objetivos:**
- Reservas recurrentes, notificaciones, capacidad de los espacios e integración con directorios institucionales (ver `proposal.md`).
- Gestión de la zona horaria por usuario: en esta versión todas las horas se interpretan en la zona horaria institucional.

## Decisiones

### D1. Restricción de exclusión en PostgreSQL como garantía final
Se usa una restricción `EXCLUDE USING gist (espacio_id WITH =, tsrange(inicio, fin, '[)') WITH &&) WHERE (estado IN ('PENDIENTE', 'APROBADA'))`, con la extensión `btree_gist`.
- **Motivo:** es la única garantía que no depende del código de la aplicación. Cubre también cualquier escritura futura que no pase por el servicio.
- **Alternativas descartadas:** solo validación en código (no es segura ante concurrencia); bloqueo de tabla completa (penaliza todos los espacios).

### D2. Bloqueo pesimista por espacio al crear o aprobar
Antes de validar disponibilidad, el servicio bloquea la fila del espacio con `SELECT ... FOR UPDATE` dentro de la misma transacción.
- **Motivo:** serializa las solicitudes del mismo espacio y permite devolver un 409 con un mensaje claro en lugar de una excepción de base de datos.
- **Alternativa descartada:** bloqueo optimista con versión. Obliga a reintentos y no evita el solapamiento por sí solo.

### D3. Traducción de conflictos a 409
La excepción de violación de exclusión se captura en el servicio o en el manejador global de errores y se responde con 409, igual que la validación previa.

### D4. Intervalos semiabiertos y bloques de 30 minutos
Los intervalos son `[inicio, fin)` y deben alinearse con bloques de 30 minutos.
- **Motivo:** permite reservas contiguas sin solapamiento y simplifica la validación.
- **Supuesto (A-02 del plan):** bloques de 30 minutos. Si se requiere otra granularidad, se cambia la validación del servicio y la restricción CHECK asociada.

### D5. Zona horaria y almacenamiento
Las horas se guardan como `TIMESTAMPTZ` en UTC. La interpretación de las horas de entrada usa la zona horaria institucional, configurada en la aplicación.
- **Supuesto (A-01 del plan):** una única zona horaria institucional.

### D6. Regla de cuatro horas por reserva
La duración se valida en el servicio (`fin - inicio <= 4 h`) y se refuerza con una restricción CHECK en la base de datos.
- **Decisión:** la regla se aplica por reserva individual, tal como quedó decidido en `PLAN_INGENIERIA.md`.
- **Limitación conocida:** dos reservas contiguas del mismo usuario en el mismo espacio pueden sumar más de cuatro horas (ver Riesgos).

### D7. Estados y transiciones
Estados: `PENDIENTE`, `APROBADA`, `RECHAZADA`, `CANCELADA`. Las transiciones válidas son:
- `PENDIENTE` → `APROBADA` (aprobador o administrador, distinto del creador).
- `PENDIENTE` → `RECHAZADA` (aprobador o administrador, con motivo).
- `PENDIENTE` o `APROBADA` → `CANCELADA` (propietario, aprobador o administrador).

Cada transición se valida en el servicio y se registra en `HistorialReserva` en la misma transacción.

### D8. Autorización en dos niveles
- **Filtro de seguridad** de Spring Security: exige un JWT válido y controla el acceso por rol a cada endpoint.
- **Servicio:** comprueba la propiedad de la reserva (propietario, creador distinto del aprobador) y el estado antes de actuar.
- **Motivo:** el filtro no conoce el propietario de cada recurso. Ambas comprobaciones son necesarias.

### D9. Supuestos sobre usuarios y arranque
- **A-04:** un aprobador puede crear reservas, pero no aprobar las suyas.
- **A-05:** el administrador puede crear reservas.
- **A-08:** no se permiten reservas con inicio en el pasado.
- **A-10:** el primer administrador se crea mediante un seed controlado fuera de la API pública.
- **A-11:** no hay registro público.
- **A-12:** al desactivar un usuario, sus reservas PENDIENTES pasan a CANCELADA y sus APROBADAS se mantienen en el calendario.

### D10. Contraseñas y tokens
BCrypt para las contraseñas. Los tokens JWT llevan caducidad corta y el secreto se toma de variables de entorno.

## Riesgos / Compromisos

- **[Riesgo] Elusión de la regla de cuatro horas con reservas contiguas** → Mitigación: decidir antes de la fase de tareas si la regla debe considerar las reservas contiguas del mismo usuario en el mismo espacio y día. Hasta entonces, la especificación refleja la decisión actual y lo deja explícito en un escenario.
- **[Riesgo] Doble reserva por concurrencia** → Mitigación: D1 y D2 combinados, más pruebas de concurrencia contra PostgreSQL real.
- **[Riesgo] Cambios de horario de verano** → Mitigación: `TIMESTAMPTZ` y pruebas con fechas cercanas al cambio de hora.
- **[Compromiso] Bloqueo por espacio** → Las solicitudes del mismo espacio se serializan. Es aceptable por el volumen esperado de un sistema universitario.

## Plan de migración

No hay datos previos. Se crea el esquema con migraciones Flyway versionadas. Para revertir, se usa una migración de retroceso o una base nueva durante el desarrollo.
