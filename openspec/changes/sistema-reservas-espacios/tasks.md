# Tareas

## 1. Base del proyecto

- [x] 1.1 Crear el proyecto Spring Boot 3 con Maven en Java 21, con la estructura de paquetes controller, service, repository, entity, dto, security y exception. Verificar con `mvn -q verify` que compila sin pruebas.
- [x] 1.2 Configurar PostgreSQL, Flyway y la extensión `btree_gist` en una migración inicial. Verificar que Flyway aplica las migraciones en una base limpia.
- [x] 1.3 Crear el proyecto React con Vite y TypeScript. Verificar con `npm run build` que compila.
- [x] 1.4 Configurar la base de datos PostgreSQL de pruebas (`reservas_test`) para las pruebas de integración y verificar que Flyway aplica las migraciones en ella. *Actualizada desde Testcontainers: no hay Docker en el equipo, se usa el PostgreSQL local.*

## 2. Usuarios, roles y autenticación

- [x] 2.1 Implementar las entidades `Usuario` y el enumerado de roles, con el repositorio y la restricción de correo único. Verificar con una prueba de integración que el correo duplicado falla.
- [x] 2.2 Implementar el servicio de alta y desactivación de usuarios, con validación de rol y de correo único. Verificar con pruebas JUnit y Mockito de los escenarios de `usuarios-y-roles`.
- [x] 2.3 Implementar el hash BCrypt de contraseñas y el inicio de sesión con emisión de JWT con caducidad. Verificar con pruebas de credenciales correctas, incorrectas y de usuario desactivado.
- [x] 2.4 Implementar el filtro JWT y la configuración de Spring Security con la matriz de permisos por rol. Verificar con pruebas de seguridad (MockMvc) que cada rol recibe 200, 401 o 403 según la matriz.
- [x] 2.5 Implementar el seed del primer administrador fuera de la API pública. Verificar con una prueba que no existe ningún endpoint público de registro.
- [x] 2.6 Implementar la desactivación de usuarios con cancelación de sus reservas PENDIENTES. Verificar con prueba de integración que las APROBADAS se mantienen y las PENDIENTES pasan a CANCELADA.

## 3. Espacios académicos

- [x] 3.1 Implementar las entidades `Espacio` y el enumerado de tipos, con restricción de nombre único. Verificar con prueba de integración del nombre duplicado.
- [x] 3.2 Implementar el servicio y el controlador de alta, modificación, desactivación y consulta de espacios. Verificar con pruebas JUnit y MockMvc de los escenarios de `espacios-academicos`.

## 4. Reservas: creación y reglas de negocio

- [x] 4.1 Implementar las entidades `Reserva` y `HistorialReserva`, con el enumerado de estados y la migración que añade la restricción `EXCLUDE` sobre `tsrange(inicio, fin, '[)')` para estados PENDIENTE y APROBADA. Verificar con prueba de integración que dos inserciones solapadas en el mismo espacio fallan.
- [x] 4.2 Añadir la restricción CHECK de duración máxima de cuatro horas y de fin posterior al inicio. Verificar con prueba de integración que la base de datos rechaza una reserva de 4 horas y 30 minutos.
- [x] 4.3 Implementar la validación de solicitud en el servicio: fin posterior al inicio, inicio futuro, bloques de 30 minutos, espacio activo y duración máxima. Verificar con pruebas JUnit y Mockito de cada escenario de `reservas`.
- [x] 4.4 Implementar la validación de solapamiento con bloqueo pesimista del espacio (`SELECT ... FOR UPDATE`) y la traducción de conflictos a 409. Verificar con pruebas de intervalos contiguos, solapados, de otros espacios y de reservas cerradas.
- [x] 4.5 Implementar el endpoint de creación de solicitudes en estado PENDIENTE, con la regla de que el creador es el usuario autenticado. Verificar con MockMvc que la respuesta incluye el estado PENDIENTE.
- [x] 4.6 Verificar la consistencia ante concurrencia con una prueba que lance dos solicitudes simultáneas para el mismo espacio e intervalo contra PostgreSQL real. Verificar que exactamente una queda registrada y la otra recibe 409, repitiendo la prueba 20 veces.

## 5. Reservas: consulta, cancelación e historial

- [x] 5.1 Implementar la consulta de reservas por rol: solicitante solo las propias, aprobador y administrador todas. Verificar con pruebas de MockMvc para cada rol.
- [x] 5.2 Implementar la cancelación de reservas PENDIENTE o APROBADA, con permisos de propietario, aprobador o administrador. Verificar con pruebas de propietario, ajeno, y de reserva cerrada.
- [x] 5.3 Implementar el registro de historial en la misma transacción que cada cambio de estado, y el endpoint de consulta de historial con permisos de propietario, aprobador o administrador. Verificar con prueba de integración que un fallo en el historial revierte el cambio.

## 6. Aprobación y rechazo

- [x] 6.1 Implementar la aprobación de solicitudes PENDIENTE por aprobador o administrador distinto del creador, con revalidación de disponibilidad bajo bloqueo del espacio. Verificar con pruebas de aprobación ajena, propia y de solicitud no pendiente.
- [x] 6.2 Implementar el rechazo con motivo obligatorio. Verificar con pruebas de rechazo con motivo, sin motivo y de solicitud no pendiente.
- [x] 6.3 Verificar la decisión concurrente sobre una misma solicitud con una prueba de aprobación y rechazo simultáneos contra PostgreSQL real. Verificar que solo una decisión se aplica y la otra recibe 409.

## 7. Frontend React

- [x] 7.1 Implementar el inicio de sesión, el almacenamiento de la sesión y las rutas protegidas. Verificar con pruebas de Vitest y React Testing Library de la redirección sin sesión.
- [x] 7.2 Implementar las vistas de espacios y de creación y consulta de reservas. Verificar con pruebas de formulario que rechazan horas no alineadas a 30 minutos y duraciones de más de 4 horas antes de enviar.
- [x] 7.3 Implementar las vistas de aprobación, rechazo, cancelación e historial, mostrando solo las acciones permitidas por rol. Verificar con pruebas que cada rol ve solo sus acciones.

## 8. Verificación de integración

- [x] 8.1 Ejecutar la suite completa de backend y frontend. Verificar que todas las pruebas de los grupos 2 a 7 pasan en verde: 71 pruebas de backend (con PostgreSQL real) y 20 de frontend.
- [x] 8.2 Verificar que cada escenario de `specs/` tiene al menos una prueba automatizada, revisando las pruebas por nombre de escenario. La tabla formal de trazabilidad se completa en la Fase 6.

## Flujo de trabajo posterior

- Crear un commit por grupo de tareas, con mensajes claros, después de que cada verificación pase.
- Revisar el código generado antes de aceptarlo.
- Archivar el cambio con `/opsx-archive` cuando la verificación esté completa.
