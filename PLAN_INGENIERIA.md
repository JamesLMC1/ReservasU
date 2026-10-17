# PLAN DE INGENIERÍA — Reservas de Espacios Académicos

**Estado:** Borrador para aprobación (Fase 1)
**Qué cubre:** arquitectura, tecnologías, reglas de negocio, riesgos y pruebas.
**Qué no cubre:** código funcional, especificaciones (Fase 2) y tareas (Fase 3).

---

## 1. ¿Qué vamos a construir?

Un sistema web para que la universidad gestione la reserva de espacios académicos (laboratorios, auditorios y salas de cómputo). Debe:

- Dejar pedir reservas y consultar su estado.
- Impedir reservas solapadas en el mismo espacio, aunque se pidan a la vez.
- Dar permisos distintos según el rol del usuario.
- Guardar aprobaciones, rechazos, cancelaciones e historial.

---

## 2. ¿Por qué Java en el backend?

- Es un lenguaje maduro y muy usado en sistemas de gestión como este (universidades, empresas).
- Spring Boot trae hecho lo que necesitamos: API REST, seguridad, acceso a base de datos y validaciones.
- El código se organiza en capas claras (controladores, servicios, repositorios), fácil de mantener y de repartir entre varias personas.
- Tiene un ecosistema enorme de pruebas (JUnit, Mockito) y mucha documentación.
- Al ser tipado y compilado, muchos errores se detectan al construir y no en producción.

**En una frase:** Java + Spring Boot nos da una base sólida, probada y mantenible para un sistema con reglas de negocio y concurrencia.

---

## 3. ¿Por qué React en el frontend?

- React es la biblioteca más usada para interfaces web: hay muchísimos ejemplos y ayuda disponible.
- Vite arranca y compila muy rápido.
- TypeScript añade tipos: avisa de errores al escribir y hace más seguras las llamadas a la API.
- Organizamos una página por sección (reservas, espacios, aprobaciones, usuarios) reutilizando componentes.

**Importante:** la interfaz solo oculta acciones según el rol. La seguridad real se aplica en el backend.

---

## 4. Arquitectura

**Monolito modular:** backend Spring Boot + frontend React + PostgreSQL.

```
[ React SPA ]  --HTTPS/JSON + JWT-->  [ Spring Boot API ]  --JDBC/JPA-->  [ PostgreSQL ]
```

**¿Por qué no microservicios?** Es un sistema pequeño, con un único equipo y un único despliegue. Los microservicios añadirían complejidad (red, despliegues, transacciones distribuidas) sin beneficio para este alcance.

**Backend** (paquete base `es.uni.reservas`):

```
controller/  endpoints REST
service/     reglas de negocio y transacciones
repository/  acceso a datos (Spring Data JPA)
entity/      entidades y enumerados
dto/         entrada y salida de la API
security/    Spring Security y filtro JWT
config/      seguridad, reloj y administrador inicial
exception/   errores de dominio y manejo global
```

Los controladores no acceden directamente a los repositorios; las reglas viven en los servicios.

**Frontend:**

```
src/
  api/          cliente HTTP y tipos
  auth/         sesión y rutas protegidas
  paginas/      una página por sección
  componentes/  componentes reutilizables
```

---

## 5. Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21 (LTS) + Spring Boot 3 + Maven |
| Seguridad | Spring Security + JWT (jjwt) |
| Base de datos | PostgreSQL + Spring Data JPA (Hibernate) |
| Migraciones | Flyway (versiona el esquema, reproducible en todos los entornos) |
| Frontend | React + Vite + TypeScript |
| Pruebas | JUnit 5 + Mockito (unitarias) y Spring Boot Test contra PostgreSQL local (integración) |
| Especificaciones | OpenSpec |
| Versiones | Git + GitHub |

**Nota:** las versiones exactas se fijan en `pom.xml` y `package.json` y se documentan en cada README.

---

## 6. Modelo de datos

| Entidad | Campos principales |
|---|---|
| `Usuario` | id, nombre, correo (único), contraseña (hash), rol, activo |
| `Espacio` | id, nombre (único), tipo (LABORATORIO / AUDITORIO / SALA_COMPUTO), activo |
| `Reserva` | id, usuario_id, espacio_id, inicio, fin, estado, motivo_rechazo, creada_en |
| `HistorialReserva` | id, reserva_id, estado_anterior, estado_nuevo, usuario_id, fecha, detalle |

---

## 7. Estados e intervalos

```
PENDIENTE --aprobar--> APROBADA --cancelar--> CANCELADA
    |--rechazar--> RECHAZADA
    |--cancelar--> CANCELADA
```

- Una reserva **PENDIENTE** o **APROBADA** ocupa el espacio; **RECHAZADA** y **CANCELADA** no.
- Las transiciones válidas se validan en el servicio, no solo en la base de datos.
- Las reservas usan `inicio` y `fin` con zona horaria (`TIMESTAMPTZ`) y el intervalo es semiabierto `[inicio, fin)`: una reserva que acaba a las 12:00 no choca con otra que empieza a las 12:00.
- Las horas deben ser múltiplos de 30 minutos.

---

## 8. Reglas de negocio

| ID | Regla | Dónde se valida |
|---|---|---|
| RN-01 | `fin` debe ser posterior a `inicio` | Servicio y CHECK en BD |
| RN-02 | Duración máxima de 4 horas por reserva | Servicio y CHECK en BD |
| RN-03 | Sin solapamiento con reservas PENDIENTE o APROBADA del mismo espacio | Servicio + EXCLUDE en BD |
| RN-04 | Solo se pueden reservar espacios activos | Servicio |
| RN-05 | Solo se aprueban o rechazan reservas PENDIENTE | Servicio |
| RN-06 | Un rechazo exige motivo | Servicio |
| RN-07 | Un solicitante solo cancela sus propias reservas | Servicio |
| RN-08 | Cada cambio de estado se registra en el historial | Servicio (misma transacción) |

### ¿Cómo garantizamos que no haya solapamientos?

Tres mecanismos combinados (RN-03):

1. **Validación en el servicio**, para devolver un error claro (409) antes de tocar la base.
2. **Bloqueo pesimista** de la fila del espacio (`SELECT ... FOR UPDATE`) al crear o aprobar, que serializa las peticiones del mismo espacio.
3. **Restricción `EXCLUDE` en PostgreSQL** (`btree_gist`): la última línea de defensa. Aunque dos transacciones pasen la validación a la vez, la base rechaza la segunda.

La validación en código sola no basta ante concurrencia; la restricción de base garantiza la regla aunque cambie el código.

---

## 9. Permisos por rol

| Operación | Solicitante | Aprobador | Administrador |
|---|:-:|:-:|:-:|
| Crear reserva propia | Sí | Sí | Sí |
| Consultar reservas propias | Sí | Sí | Sí |
| Consultar todas las reservas | No | Sí | Sí |
| Aprobar / rechazar | No | Sí | Sí |
| Cancelar reserva propia | Sí | Sí | Sí |
| Cancelar reserva ajena | No | Sí | Sí |
| Consultar historial | Solo propia | Sí | Sí |
| Gestionar espacios | No | No | Sí |
| Gestionar usuarios y roles | No | No | Sí |

**Pendiente de confirmar:** si el aprobador puede crear reservas propias y si se aprueban solas (ver A-04).

---

## 10. Decisiones pendientes

Propuesta por defecto para cada una; hay que confirmarla o cambiarla antes de la Fase 2.

| ID | Pregunta | Propuesta |
|---|---|---|
| A-01 | ¿Zona horaria única? | Sí, la institucional (`Europe/Madrid`). |
| A-02 | ¿Granularidad de las reservas? | Bloques de 30 minutos. |
| A-03 | ¿Reservas recurrentes? | No en la primera versión. |
| A-04 | ¿Un aprobador puede crear y aprobar sus reservas? | Puede crear, pero no aprobar las propias. |
| A-05 | ¿El administrador puede crear reservas? | Sí. |
| A-06 | ¿Capacidad máxima de los espacios? | No en la primera versión. |
| A-07 | ¿Notificaciones por correo? | No en la primera versión. |
| A-08 | ¿Reservas en el pasado? | No se permite que el inicio ya haya pasado. |
| A-09 | ¿Antelación mínima o máxima? | Sin límite en la primera versión. |
| A-10 | ¿Primer administrador? | Seed controlado, fuera de la API pública. |
| A-11 | ¿Registro público de usuarios? | No; los crea el administrador. |
| A-12 | ¿Reservas de un usuario desactivado? | Las PENDIENTES se cancelan; las APROBADAS se mantienen. |

### Ambigüedad principal: la regla de 4 horas (R-03)

El enunciado dice que **nadie puede reservar un espacio más de 4 horas consecutivas**. La decisión es aplicarla **por reserva**. Eso deja una vía: dos reservas contiguas de 4 horas dejan 8 horas seguidas.

**Decisión pendiente:** ¿debe considerar también las reservas contiguas del mismo usuario en el mismo espacio y día? Es lo más importante para la Fase 3.

---

## 11. Riesgos

| ID | Riesgo | Prob. | Impacto | Mitigación |
|---|---|:-:|:-:|---|
| R-01 | Doble reserva por concurrencia | Media | Alto | EXCLUDE en BD, bloqueo pesimista y pruebas de concurrencia. |
| R-02 | Filtración de datos por permisos solo en frontend | Media | Alto | Autorización en servicios y pruebas por rol. |
| R-03 | Elusión de la regla de 4 horas con reservas contiguas | Alta | Medio | Decidir en Fase 2 y añadir pruebas específicas. |
| R-04 | Ambigüedades producen especificaciones incorrectas | Media | Alto | Resolver la sección 10 antes de la Fase 2. |
| R-05 | Código generado por IA sin revisar | Media | Medio | Revisión humana, pruebas y commits pequeños. |
| R-06 | Desviación entre especificaciones y código | Media | Medio | Actualizar OpenSpec antes de cambiar reglas de negocio. |
| R-07 | Zona horaria o cambio de hora | Media | Medio | `TIMESTAMPTZ` y pruebas cerca del cambio de hora. |
| R-08 | Secreto JWT o contraseñas mal gestionados | Baja | Alto | Secretos por variables de entorno, nunca en el repo. |
| R-09 | Alcance creciente | Media | Medio | Mantener fuera lo no decidido hasta una nueva fase. |

---

## 12. Pruebas

| Nivel | Herramienta | Qué cubre |
|---|---|---|
| Unitarias (servicios) | JUnit 5 + Mockito | Reglas RN-01 a RN-08, estados y permisos de dominio. |
| Unitarias (seguridad) | JUnit 5 + Spring Security Test | Acceso permitido o denegado por rol. |
| Integración (API) | Spring Boot Test + MockMvc | Contratos REST, códigos HTTP, validación, JWT. |
| Integración (base) | Spring Boot Test contra PostgreSQL local | EXCLUDE, CHECK y migraciones Flyway. |
| Concurrencia | Varios hilos contra PostgreSQL | Dos solicitudes simultáneas: solo una queda activa. |
| Frontend | Vitest + React Testing Library | Renderizado por rol y validación de formularios. |

**Criterio de aceptación de la Fase 4:** todas las reglas RN tienen pruebas en verde y las de concurrencia se ejecutan de forma repetible.

---

## 13. Trazabilidad y plan de trabajo

Cada requisito se relaciona con su especificación (Fase 2), su código (Fase 4) y sus pruebas (Fase 4). La matriz final se recoge en `INFORME_FINAL.md` (Fase 6).

Commits por etapa, en este orden:

1. `docs: añadir PLAN_INGENIERIA.md` (tras la aprobación).
2. `chore(openspec): inicializar OpenSpec`.
3. `docs(openspec): revisar ambigüedades y tareas`.
4. Commits pequeños por funcionalidad (p. ej. `feat(reservas): validar duración máxima de 4 horas`).
5. `feat(openspec): cambio de duración máxima` (Fase 5).
6. `docs: README e INFORME_FINAL` (Fase 6).

Ramas: `main` estable; trabajo en ramas de funcionalidad con revisión antes de fusionar.

---

## 14. Requisitos funcionales (resumen)

| ID | Requisito |
|---|---|
| RF-01 | Registrar, consultar, activar y desactivar usuarios |
| RF-02 | Asignar rol: administrador, aprobador o solicitante |
| RF-03 | Registrar, modificar y desactivar espacios |
| RF-04 | Crear solicitudes de reserva |
| RF-05 | Consultar reservas propias o todas según el rol |
| RF-06 | Verificar disponibilidad y bloquear solapamientos |
| RF-07 | Aprobar o rechazar pendientes, con motivo al rechazar |
| RF-08 | Cancelar reservas |
| RF-09 | Consultar historial de cambios de estado |
| RF-10 | Aplicar la regla de máximo 4 horas por reserva |
| RF-11 | Aplicar permisos en el backend en cada operación |

---

## 15. Puntos que necesito que confirmes

1. Resolver la **ambigüedad R-03** (sección 10): ¿la regla de 4 horas es por reserva o también por usuario, espacio y día?
2. Confirmar o cambiar las **propuestas por defecto** de A-01 a A-12.
3. Confirmar **Java 21 + Spring Boot 3** y el uso de **PostgreSQL local** para las pruebas de integración.
4. Confirmar el **paquete base** `es.uni.reservas`.
