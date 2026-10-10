# PLAN DE INGENIERÍA — Sistema de Gestión de Reservas de Espacios Académicos

**Estado:** Borrador para aprobación (Fase 1)
**Alcance de este documento:** arquitectura, tecnologías, módulos, reglas de negocio, riesgos y estrategia de pruebas.
**Fuera de alcance:** código funcional, inicialización de especificaciones (Fase 2) y tareas de implementación (Fase 3).

---

## 1. Objetivo

Construir un sistema web para que una universidad gestione la reserva de espacios académicos (laboratorios, auditorios y salas de cómputo). El sistema debe:

- Permitir que los usuarios soliciten reservas y consulten su estado.
- Impedir reservas solapadas sobre un mismo espacio, incluso ante solicitudes simultáneas.
- Aplicar control de permisos según el rol.
- Registrar aprobaciones, rechazos, cancelaciones e historial.

---

## 2. Decisiones ya tomadas

| Tema | Decisión | Motivo |
|---|---|---|
| Autenticación | Login propio con Spring Security y JWT | Decisión del equipo. Sin dependencia de un directorio institucional en esta fase. |
| Regla de 4 horas | Se aplica a cada reserva individual: ninguna reserva puede durar más de 4 horas consecutivas | Decisión del equipo. Ver riesgo R-03 sobre la división de reservas. |
| Aprobación | La realizan los usuarios con rol **aprobador** (el administrador también puede hacerlo) | Decisión del equipo. |
| Momento de OpenSpec | El cambio de OpenSpec se crea después de aprobar este plan | Sigue el orden de fases del enunciado. |

---

## 3. Requisitos funcionales (resumen)

| ID | Requisito | Módulo |
|---|---|---|
| RF-01 | Registrar, consultar, activar y desactivar usuarios | Usuarios |
| RF-02 | Asignar uno de tres roles: administrador, aprobador, solicitante | Usuarios / Seguridad |
| RF-03 | Registrar, modificar y desactivar espacios de tres tipos: laboratorio, auditorio, sala de cómputo | Espacios |
| RF-04 | Crear solicitudes de reserva indicando espacio, fecha, hora de inicio y hora de fin | Reservas |
| RF-05 | Consultar solicitudes propias (solicitante) o todas (aprobador y administrador) | Reservas |
| RF-06 | Verificar disponibilidad y bloquear solapamientos en el momento de crear o aprobar | Reservas / Disponibilidad |
| RF-07 | Aprobar o rechazar solicitudes pendientes, con motivo en caso de rechazo | Aprobaciones |
| RF-08 | Cancelar reservas (por el solicitante dueño o por aprobador/administrador) | Reservas |
| RF-09 | Consultar historial de cambios de estado de cada reserva | Historial |
| RF-10 | Aplicar la regla de máximo 4 horas consecutivas por reserva | Reservas (regla de negocio) |
| RF-11 | Aplicar control de permisos en el backend para cada operación | Seguridad |

---

## 4. Requisitos no funcionales

- **Consistencia:** ninguna combinación de solicitudes concurrentes puede dejar dos reservas activas solapadas en el mismo espacio.
- **Seguridad:** contraseñas con hash (BCrypt), tokens JWT con caducidad, validación de permisos en el servidor (nunca solo en React).
- **Mantenibilidad:** capas separadas (controladores, servicios, repositorios, entidades).
- **Trazabilidad:** cada requisito tiene un ID y se relaciona con especificaciones, código y pruebas.
- **Calidad:** cobertura de pruebas unitarias en servicios de reglas de negocio; pruebas de integración para consistencia de base de datos.

---

## 5. Arquitectura

### 5.1 Estilo general

**Monolito modular** con backend en Spring Boot y frontend en React como aplicación independiente (SPA) que consume una API REST.

**Justificación:** es un sistema de tamaño acotado, con un único equipo y un único despliegue. Una arquitectura de microservicios añadiría complejidad operativa (red, despliegues, transacciones distribuidas) sin beneficio para este alcance. La separación por módulos dentro del monolito permite evolucionar más adelante.

### 5.2 Diagrama lógico

```
[ React SPA ]  --HTTPS/JSON + JWT-->  [ Spring Boot API ]  --JDBC/JPA-->  [ PostgreSQL ]
                                       ├─ Controladores (REST)
                                       ├─ Servicios (reglas de negocio)
                                       ├─ Repositorios (Spring Data JPA)
                                       ├─ Entidades (JPA)
                                       └─ Seguridad (Spring Security + JWT)
```

### 5.3 Organización del backend

Paquete base sugerido: `es.uni.reservas` (a confirmar).

```
controller/   -> endpoints REST, validación de entrada, mapeo a DTO
service/      -> reglas de negocio, transacciones, control de permisos de dominio
repository/   -> acceso a datos (Spring Data JPA)
entity/       -> entidades JPA y enumeraciones
dto/          -> objetos de entrada y salida de la API
security/     -> configuración de Spring Security, filtro JWT
exception/    -> excepciones de dominio y manejador global de errores
```

Los controladores nunca acceden a repositorios directamente. Las reglas de negocio viven en los servicios.

### 5.4 Organización del frontend

```
src/
  api/          -> cliente HTTP y funciones por recurso
  auth/         -> contexto de sesión, rutas protegidas
  features/     -> usuarios, espacios, reservas, aprobaciones, historial
  components/   -> componentes reutilizables
```

La interfaz oculta acciones según el rol para mejorar la experiencia, pero la autorización real se aplica en el backend.

---

## 6. Tecnologías

| Capa | Tecnología | Justificación |
|---|---|---|
| Backend | Java 21 (LTS) + Spring Boot 3 + Maven | Requisito del enunciado. Java 21 es LTS y compatible con Spring Boot 3.x. |
| Seguridad | Spring Security + JWT (jjwt) | Estándar de facto en Spring para autenticación sin estado. |
| Persistencia | Spring Data JPA (Hibernate) + PostgreSQL | Requisito del enunciado. JPA reduce código repetitivo. |
| Migraciones | Flyway | Versionado del esquema de base de datos, reproducible en todos los entornos. |
| Frontend | React + Vite + TypeScript | Requisito del enunciado (React). Vite ofrece un arranque rápido; TypeScript reduce errores en la comunicación con la API. |
| Pruebas backend | JUnit 5 + Mockito | Requisito del enunciado. |
| Pruebas integración | Spring Boot Test + Testcontainers (PostgreSQL real) | Ver sección 9. Requiere confirmación (ver sección 11). |
| Control de versiones | Git + GitHub | Requisito del enunciado. |
| Especificaciones | OpenSpec | Requisito del enunciado. |

**Nota de versiones:** las versiones exactas se fijarán en el `pom.xml` y en `package.json` durante la Fase 4 y se documentarán en el README.

---

## 7. Modelo de datos (preliminar)

| Entidad | Campos principales | Notas |
|---|---|---|
| `Usuario` | id, nombre, correo (único), contraseña (hash), rol, activo | Un usuario tiene un rol. |
| `Espacio` | id, nombre (único), tipo (LABORATORIO / AUDITORIO / SALA_COMPUTO), activo | — |
| `Reserva` | id, usuario_id, espacio_id, inicio, fin, estado, motivo_rechazo, creada_en | Ver sección 8.2. |
| `HistorialReserva` | id, reserva_id, estado_anterior, estado_nuevo, usuario_id, fecha, detalle | Registro de auditoría de cada transición. |

## 8. Estados, consistencia e intervalos

### 8.1 Estados de una reserva

```
PENDIENTE --aprobar--> APROBADA --cancelar--> CANCELADA
    |                     ^
    |--rechazar--> RECHAZADA
    |
    |--cancelar--> CANCELADA
```

- Una reserva **PENDIENTE** o **APROBADA** ocupa el espacio. Las **RECHAZADA** y **CANCELADA** no.
- Las transiciones válidas se validan en el servicio, no solo en la base de datos.

### 8.2 Garantía de no solapamiento (consistencia)

Se combinan tres mecanismos:

1. **Restricción de exclusión en PostgreSQL:** `EXCLUDE USING gist (espacio_id WITH =, tsrange(inicio, fin) WITH &&) WHERE (estado IN ('PENDIENTE','APROBADA'))`, con la extensión `btree_gist`. Es la última línea de defensa: aunque dos transacciones pasen la validación a la vez, la base de datos rechaza la segunda.
2. **Bloqueo pesimista** de la fila del espacio (`SELECT ... FOR UPDATE`) al crear o aprobar una reserva, para que la validación de disponibilidad y la escritura ocurran en serie por espacio.
3. **Validación previa en el servicio** para devolver un error claro (409 Conflict) en lugar de una excepción de base de datos.

**Justificación:** la validación en código por sí sola no basta ante concurrencia. La restricción de base de datos garantiza la regla incluso si cambia el código o se añade otro punto de entrada.

### 8.3 Intervalos

- Las reservas se expresan con `inicio` y `fin` como instantes con zona horaria (`TIMESTAMPTZ`).
- El intervalo es semiabierto `[inicio, fin)`: una reserva de 10:00 a 12:00 no choca con otra de 12:00 a 14:00.
- La duración se calcula como `fin - inicio` y debe ser mayor que cero.

---

## 9. Reglas de negocio y validaciones críticas

| ID | Regla | Dónde se valida |
|---|---|---|
| RN-01 | `fin` debe ser posterior a `inicio` | Servicio (y restricción CHECK en BD) |
| RN-02 | Duración máxima de 4 horas por reserva (`fin - inicio <= 4h`) | Servicio (y CHECK en BD) |
| RN-03 | No puede existir solapamiento con reservas PENDIENTE o APROBADA del mismo espacio | Servicio + restricción EXCLUDE en BD |
| RN-04 | Solo se pueden reservar espacios activos | Servicio |
| RN-05 | Solo se pueden aprobar o rechazar reservas PENDIENTE | Servicio |
| RN-06 | Un rechazo exige motivo | Servicio |
| RN-07 | Un solicitante solo cancela sus propias reservas | Servicio |
| RN-08 | Cada transición de estado se registra en el historial | Servicio (misma transacción) |

**Pruebas dirigidas por reglas:** cada regla tiene al menos una prueba unitaria y, cuando aplica, una prueba de integración.

---

## 10. Matriz de permisos (propuesta)

| Operación | Solicitante | Aprobador | Administrador |
|---|:-:|:-:|:-:|
| Crear reserva propia | Sí | Sí | Sí |
| Consultar reservas propias | Sí | Sí | Sí |
| Consultar todas las reservas | No | Sí | Sí |
| Aprobar / rechazar | No | Sí | Sí |
| Cancelar reserva propia | Sí | Sí | Sí |
| Cancelar reserva ajena | No | Sí | Sí |
| Consultar historial de una reserva | Solo propia | Sí | Sí |
| Gestionar espacios | No | No | Sí |
| Gestionar usuarios y roles | No | No | Sí |

**Pendiente de confirmar:** si el aprobador puede crear sus propias reservas y si estas se aprueban automáticamente (ver sección 11).

---

## 11. Ambigüedades abiertas

Estas decisiones afectan a las especificaciones (Fase 2). Se propone una respuesta por defecto para cada una; el usuario debe confirmarla o cambiarla.

| ID | Pregunta | Propuesta por defecto |
|---|---|---|
| A-01 | ¿Hay zona horaria única para toda la universidad? | Sí. Zona horaria del servidor y de la institución, por ejemplo `Europe/Madrid` o la que corresponda. |
| A-02 | ¿Qué granularidad tienen las reservas (minutos, medias horas, horas completas)? | Bloques de 30 minutos. |
| A-03 | ¿Las reservas pueden ser recurrentes (cada semana, por ejemplo)? | No en la primera versión. |
| A-04 | ¿Un aprobador puede crear reservas y aprobarlas él mismo? | Sí puede crear reservas, pero no aprobar las propias. |
| A-05 | ¿El administrador puede crear reservas? | Sí. |
| A-06 | ¿Tienen los espacios capacidad máxima de personas? | No en la primera versión. |
| A-07 | ¿Se envían notificaciones por correo al cambiar el estado? | No en la primera versión. |
| A-08 | ¿Qué hace la aplicación con reservas en el pasado? | No se permite crear reservas cuyo inicio ya haya pasado. |
| A-09 | ¿Hay antelación mínima o máxima para reservar? | Sin límite en la primera versión. |
| A-10 | ¿Cómo se crea el primer administrador? | Mediante un script o seed inicial controlado fuera de la API pública. |
| A-11 | ¿El registro de usuarios es público? | No. Lo crea el administrador. |
| A-12 | ¿Qué ocurre con las reservas de un usuario que se desactiva? | Se mantienen en el historial. Las pendientes pasan a cancelarse. |

### Contradicción detectada: regla de 4 horas (R-03)

El enunciado dice que **ningún usuario podrá reservar un espacio durante más de cuatro horas consecutivas**. La decisión tomada es aplicarla **por reserva**. Eso deja abierta una vía: un usuario puede hacer dos reservas consecutivas de 4 horas cada una y ocupar 8 horas seguidas.

**Decisión pendiente:** ¿la regla debe considerar también las reservas contiguas del mismo usuario en el mismo espacio y día? Esta es la ambigüedad más importante para la Fase 3.

---

## 12. Riesgos

| ID | Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|:-:|:-:|---|
| R-01 | Doble reserva por concurrencia | Media | Alto | Restricción EXCLUDE en BD, bloqueo pesimista y pruebas de concurrencia (sección 9). |
| R-02 | Filtración de datos por permisos solo en frontend | Media | Alto | Autorización en servicios y pruebas de permisos por rol. |
| R-03 | Elusión de la regla de 4 horas con reservas contiguas | Alta | Medio | Decidir en Fase 2 (sección 11). Pruebas específicas. |
| R-04 | Ambigüedades en el enunciado producen especificaciones incorrectas | Media | Alto | Resolver la sección 11 antes de la Fase 2. |
| R-05 | Código generado por IA sin revisión adecuada | Media | Medio | Revisión humana de cada cambio, pruebas automatizadas y commits pequeños. |
| R-06 | Desviación entre especificaciones y código | Media | Medio | Actualizar OpenSpec antes de cambiar el código de reglas de negocio. Matriz de trazabilidad en la Fase 6. |
| R-07 | Problemas de zona horaria o de cambio de horario de verano | Media | Medio | Usar `TIMESTAMPTZ` y pruebas con fechas cercanas al cambio de hora. |
| R-08 | Secreto JWT o contraseñas mal gestionados | Baja | Alto | Secretos por variables de entorno, nunca en el repositorio. |
| R-09 | Alcance creciente (notificaciones, recurrencias, etc.) | Media | Medio | Mantener fuera de alcance lo listado en la sección 11 hasta una nueva fase. |

---

## 13. Estrategia de pruebas

| Nivel | Herramienta | Qué cubre |
|---|---|---|
| Unitarias (servicios) | JUnit 5 + Mockito | Reglas RN-01 a RN-08, transiciones de estado, permisos de dominio. Repositorios simulados. |
| Unitarias (seguridad) | JUnit 5 + Spring Security Test | Acceso permitido o denegado por rol en cada operación de la matriz de permisos. |
| Integración (API) | Spring Boot Test + MockMvc | Contratos REST, códigos HTTP, validación de entrada, JWT. |
| Integración (persistencia) | Testcontainers con PostgreSQL | Restricción EXCLUDE, CHECK, migraciones Flyway. |
| Concurrencia | Pruebas con varios hilos contra PostgreSQL real | Dos solicitudes simultáneas para el mismo espacio y horario: solo una debe quedar activa. |
| Frontend | Vitest + React Testing Library | Renderizado según rol y validación de formularios. |
| Regresión | Suite completa en cada commit de la Fase 5 | Garantiza que el cambio de la regla de 4 horas no rompe lo existente. |

**Criterio de aceptación de la Fase 4:** todas las reglas RN tienen pruebas en verde, y las pruebas de concurrencia se ejecutan de forma repetible.

---

## 14. Trazabilidad

Cada requisito (RF, RN) se relacionará con:

1. Su especificación en OpenSpec (Fase 2).
2. Las clases y endpoints que lo implementan (Fase 4).
3. Las pruebas que lo verifican (Fase 4).

La matriz se mantendrá en el `INFORME_FINAL.md` (Fase 6).

---

## 15. Plan de trabajo y control de versiones

Commits sugeridos por etapa, en este orden:

1. `docs: añadir PLAN_INGENIERIA.md` (commit independiente, tras la aprobación).
2. `chore(openspec): inicializar OpenSpec y definir especificaciones iniciales`.
3. `docs(openspec): revisar ambigüedades y definir tareas de implementación`.
4. Commits pequeños por funcionalidad en la Fase 4 (por ejemplo, `feat(reservas): validar duración máxima de 4 horas`).
5. `feat(openspec): cambio de duración máxima de cuatro horas` (Fase 5, con la especificación actualizada en un commit previo).
6. `docs: README e INFORME_FINAL` (Fase 6).

Ramas: `main` estable; trabajo en ramas de funcionalidad con revisión antes de fusionar.

---

## 16. Puntos que necesito que confirmes

1. Resolver la **ambigüedad R-03** (sección 11): ¿la regla de 4 horas se aplica por reserva o también por usuario, espacio y día?
2. Confirmar o cambiar las **propuestas por defecto** de A-01 a A-12.
3. Confirmar **Java 21 + Spring Boot 3**, y **Testcontainers** para las pruebas de integración con PostgreSQL.
4. Confirmar el **paquete base** del backend (`es.uni.reservas` es un ejemplo).

Cuando apruebes el plan y las decisiones, haré el commit independiente de la Fase 1 y después seguiré con la Fase 2.
