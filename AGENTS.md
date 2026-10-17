# AGENTS.md

Sistema universitario de reservas de espacios académicos. Dos proyectos **independientes** en un mismo repositorio. No hay manifiesto raíz, configuración de workspace ni CI: ejecuta todos los comandos dentro de `backend/` o `frontend/`.

## Idioma
Todo el código, identificadores, comentarios, documentación y mensajes de commit están en **español** (`Usuario`, `Reserva`, `EstadoReserva`, `aprobar`, `espacio`...). Mantén este idioma en el código nuevo.

## Comandos

### Backend — `backend/` (Java 21, Spring Boot 3.5, Maven)
- Configuración: copia `backend/.env.example` a `backend/.env`. Se carga solo con `spring-dotenv`; no lo subas al repositorio.
- Ejecutar: `mvn spring-boot:run` (API en `:8080`)
- Todas las pruebas: `mvn test`
- Una prueba: `mvn -Dtest=ReservaServiceTest test` (añade `#metodo` para un único método)
- No hay Maven Wrapper; Maven 3.9+ y JDK 21 deben estar instalados.

### Frontend — `frontend/` (React 19, Vite, TypeScript)
- `npm install` → `npm run dev` (`:5173`) → `npm test`
- `npm run build` ejecuta `tsc -b && vite build`; `tsc` es la comprobación de tipos.
- `npm run lint` usa **oxlint**, no ESLint.
- `npm test` es `vitest run`; un solo archivo: `npx vitest run src/reservas/validaciones.test.ts`
- En este equipo Windows/PowerShell, `npm.ps1` está bloqueado por la política de ejecución: usa `npm.cmd ...` o `cmd /c "npm ..."`.

## Pruebas y requisitos de base de datos
- Las pruebas de integración del backend (`src/test/.../integracion/`) se ejecutan **solo si `DB_PASSWORD` está definida** (`@EnabledIfEnvironmentVariable`); si no, se omiten en silencio. También respetan `TEST_DB_URL` (por defecto `jdbc:postgresql://localhost:5432/reservas_test`, usuario por defecto `postgres`).
- Requieren un **PostgreSQL local real** con Flyway aplicado. Testcontainers/Docker no se usa a propósito (se descartó porque el equipo no tiene Docker).
- `hibernate.ddl-auto=validate`: la fuente de verdad del esquema es `backend/src/main/resources/db/migration/` (Flyway), no las anotaciones JPA. Añade una migración nueva `V<n>__*.sql`; nunca edites una ya aplicada.

## Modelo de consistencia — no lo debilites
El no solapamiento se garantiza en tres niveles y los tres deben mantenerse:
1. Validación en el servicio (devuelve un 409 claro).
2. Bloqueo pesimista `SELECT ... FOR UPDATE` de la fila de `espacio` al crear o aprobar.
3. Restricción PostgreSQL `EXCLUDE USING gist (espacio_id WITH =, tstzrange(inicio, fin, '[)') WITH &&) WHERE (estado IN ('PENDIENTE','APROBADA'))`, que necesita la extensión `btree_gist`.

Los intervalos son semiabiertos `[inicio, fin)`; las horas deben ser múltiplos de 30 minutos; máximo 4 h por reserva. Las fechas de entrada no llevan zona y se interpretan en `APP_TIMEZONE` (por defecto `Europe/Madrid`), y se almacenan como `TIMESTAMPTZ` (UTC).

## Autenticación
Spring Security + JWT. La autorización se aplica en el backend (tanto en el filtro de seguridad **como** en los servicios); el frontend solo oculta la interfaz. Nunca trates las comprobaciones de rol de React como seguridad. Nadie puede aprobar ni rechazar una solicitud propia.

## OpenSpec
El repositorio está guiado por especificaciones. Las especificaciones del trabajo actual viven dentro de `openspec/changes/sistema-reservas-espacios/`; `openspec/specs/` queda vacío hasta archivar o sincronizar. Usa los comandos `/opsx-*` de `.opencode/commands/` (respaldados por la CLI `openspec`). Actualiza la especificación/diseño antes de cambiar el comportamiento de una regla de negocio (riesgo R-06 en `PLAN_INGENIERIA.md`).

## Convenciones
- Conventional Commits en español: `feat(reservas): ...`, `docs(openspec): ...`, `chore: ...`.
- `PLAN_INGENIERIA.md` contiene las decisiones de arquitectura aprobadas; los dos README documentan endpoints, roles y limitaciones conocidas.
- La verificación es manual (no hay CI ni hooks de pre-commit): build/comprobación de tipos, luego lint, luego pruebas, en cada proyecto.
