# Backend: Sistema de Gestión de Reservas de Espacios Académicos

API REST en Java 21 con Spring Boot 3.5, Maven, PostgreSQL y Flyway. Gestiona usuarios y roles, espacios académicos, reservas, aprobaciones e historial.

## Requisitos

- Java 21 (Temurin recomendado)
- Maven 3.9+
- PostgreSQL 13 o superior (probado con PostgreSQL 18)
- Base de datos `reservas` creada en PostgreSQL

## Configuración

1. Copia `.env.example` como `.env` dentro de `backend/`.
2. Completa los valores:

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC de la base, por ejemplo `jdbc:postgresql://localhost:5432/reservas` |
| `DB_USER` / `DB_PASSWORD` | Usuario y contraseña de PostgreSQL |
| `JWT_SECRET` | Secreto para firmar los tokens. Debe tener al menos 32 caracteres. |
| `JWT_EXPIRATION_MINUTES` | Caducidad del token en minutos (por defecto 30) |
| `APP_TIMEZONE` | Zona horaria institucional (por defecto `Europe/Madrid`) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Primer administrador. Se crea al arrancar solo si no existe ningún administrador. La contraseña debe tener al menos 8 caracteres. |
| `CORS_ORIGIN` | Origen del frontend (por defecto `http://localhost:5173`) |

El archivo `.env` se carga automáticamente al arrancar gracias a `spring-dotenv`. No lo subas al repositorio.

## Ejecución

```bash
cd backend
mvn spring-boot:run
```

Al arrancar, Flyway aplica las migraciones de `src/main/resources/db/migration`, y la API queda disponible en `http://localhost:8080`.

## Pruebas

```bash
mvn test
```

- **Unitarias** (Mockito): reglas de negocio de reservas, usuarios, espacios y autenticación, y el servicio JWT.
- **Capa web** (`@WebMvcTest`): autenticación y permisos por rol.
- **Integración** (`ReservasIntegracionTest`): consistencia ante solicitudes simultáneas y la regla de cuatro horas contra PostgreSQL real. Se ejecuta solo si `DB_PASSWORD` está definida; si no, se omite.

## Estructura

```
src/main/java/es/uni/reservas/
  controller/   Endpoints REST
  service/      Reglas de negocio y transacciones
  repository/   Acceso a datos (Spring Data JPA)
  entity/       Entidades JPA y enumerados
  dto/          Objetos de entrada y salida de la API
  security/     JWT, filtro de autenticación y usuario autenticado
  config/       Seguridad, reloj del sistema y administrador inicial
  exception/    Excepciones de dominio y manejo global de errores
src/main/resources/db/migration/   Migraciones Flyway
```

## Roles y permisos

| Rol | Puede |
|---|---|
| `ADMINISTRADOR` | Gestionar usuarios, roles y espacios. Crear, cancelar y decidir sobre reservas (no sobre las propias). |
| `APROBADOR` | Ver todas las reservas, aprobar o rechazar solicitudes ajenas y cancelar cualquier reserva. |
| `SOLICITANTE` | Crear reservas y ver, cancelar o consultar el historial de las propias. |

Los permisos se validan en la configuración de seguridad y en los servicios.

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Inicia sesión y devuelve un token JWT |
| GET | `/api/usuarios` | Administrador | Lista los usuarios |
| POST | `/api/usuarios` | Administrador | Crea un usuario con rol |
| PATCH | `/api/usuarios/{id}/rol` | Administrador | Cambia el rol de un usuario |
| PATCH | `/api/usuarios/{id}/desactivar` | Administrador | Desactiva un usuario y cancela sus solicitudes pendientes |
| GET | `/api/espacios` | Autenticado | Lista los espacios activos |
| POST | `/api/espacios` | Administrador | Crea un espacio |
| PUT | `/api/espacios/{id}` | Administrador | Modifica un espacio |
| PATCH | `/api/espacios/{id}/desactivar` | Administrador | Desactiva un espacio |
| GET | `/api/reservas` | Autenticado | Lista reservas (`?estado=PENDIENTE` opcional). Los solicitantes solo ven las propias. |
| POST | `/api/reservas` | Autenticado | Crea una solicitud en estado `PENDIENTE` |
| GET | `/api/reservas/{id}` | Propietario o aprobador | Detalle de una reserva |
| POST | `/api/reservas/{id}/cancelar` | Propietario, aprobador o administrador | Cancela una reserva activa |
| POST | `/api/reservas/{id}/aprobar` | Aprobador o administrador | Aprueba una solicitud pendiente ajena |
| POST | `/api/reservas/{id}/rechazar` | Aprobador o administrador | Rechaza con motivo obligatorio (`{"motivo": "..."}`) |
| GET | `/api/reservas/{id}/historial` | Propietario, aprobador o administrador | Historial de cambios de estado |

### Ejemplo de solicitud

```bash
curl -X POST http://localhost:8080/api/reservas \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"espacioId": 1, "inicio": "2026-10-12T10:00:00", "fin": "2026-10-12T12:00:00"}'
```

Las fechas se envían sin zona horaria y se interpretan en `APP_TIMEZONE`. Las horas deben ser múltiplos de 30 minutos.

## Reglas de negocio

- Una reserva no puede durar más de **cuatro horas consecutivas**.
- Dos reservas activas (`PENDIENTE` o `APROBADA`) no pueden solaparse en el mismo espacio. Los intervalos son semiabiertos: una reserva que termina a las 12:00 no choca con otra que empieza a las 12:00.
- No se pueden reservar fechas pasadas ni espacios desactivados.
- Ante solicitudes simultáneas, el servicio bloquea el espacio con `SELECT ... FOR UPDATE` y la base de datos rechaza cualquier solapamiento restante mediante una restricción `EXCLUDE`. El cliente recibe 409.
- Nadie puede aprobar ni rechazar una solicitud propia.
- Cada cambio de estado se registra en el historial dentro de la misma transacción.

## Errores

Las respuestas de error tienen el formato `{"mensaje": "..."}`:

| Código | Causa |
|---|---|
| 400 | Datos no válidos o regla de negocio incumplida |
| 401 | Sin token, token caducado o credenciales incorrectas |
| 403 | Rol sin permiso para la operación |
| 404 | Recurso no encontrado |
| 409 | Conflicto: solapamiento, correo o nombre duplicado, o decisión sobre una solicitud que ya no está pendiente |

## Limitaciones conocidas

- La regla de cuatro horas se aplica por reserva. Dos reservas contiguas del mismo usuario en el mismo espacio pueden sumar más de cuatro horas. Está pendiente de decisión.
- Las pruebas de integración contra PostgreSQL se omiten si no se define `DB_PASSWORD`.
- No hay notificaciones por correo, reservas recurrentes ni límite de capacidad de los espacios.
