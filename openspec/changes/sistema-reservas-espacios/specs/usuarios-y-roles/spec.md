# Especificación delta

## Purpose

Define quién puede usar el sistema, qué rol tiene cada usuario y qué operaciones puede realizar según ese rol.

## ADDED Requirements

### Requirement: Roles de usuario
El sistema SHALL asignar a cada usuario exactamente uno de los siguientes roles: administrador, aprobador o solicitante.

#### Scenario: Usuario con un rol válido
- **WHEN** se crea un usuario con el rol "aprobador"
- **THEN** el sistema guarda el usuario con ese rol

#### Scenario: Usuario sin rol o con rol desconocido
- **WHEN** se intenta crear un usuario sin rol o con un rol que no existe
- **THEN** el sistema rechaza la operación con un error de validación y no crea el usuario

### Requirement: Alta de usuarios por el administrador
El sistema SHALL permitir crear usuarios solo a los administradores. Las altas son internas; no existe registro público.

#### Scenario: Administrador crea un usuario
- **WHEN** un administrador envía los datos válidos de un nuevo usuario con correo único
- **THEN** el sistema crea el usuario y lo devuelve con su rol y su estado activo

#### Scenario: Solicitante intenta crear un usuario
- **WHEN** un solicitante intenta crear un usuario
- **THEN** el sistema rechaza la operación con un error de permisos (403)

#### Scenario: Correo duplicado
- **WHEN** un administrador intenta crear un usuario con un correo que ya existe
- **THEN** el sistema rechaza la operación con un error de conflicto (409)

### Requirement: Autenticación con contraseña
El sistema SHALL autenticar a los usuarios con correo y contraseña y SHALL emitir un token de acceso con caducidad. Las contraseñas MUST almacenarse con hash y MUST NOT almacenarse en texto plano.

#### Scenario: Inicio de sesión correcto
- **WHEN** un usuario activo envía correo y contraseña correctos
- **THEN** el sistema devuelve un token de acceso con fecha de caducidad

#### Scenario: Contraseña incorrecta
- **WHEN** un usuario envía una contraseña incorrecta
- **THEN** el sistema rechaza el inicio de sesión con un error 401 y no revela si el correo existe

#### Scenario: Usuario desactivado
- **WHEN** un usuario desactivado envía credenciales correctas
- **THEN** el sistema rechaza el inicio de sesión con un error 401

### Requirement: Desactivación de usuarios
El sistema SHALL permitir a los administradores desactivar usuarios. Un usuario desactivado MUST NOT poder iniciar sesión ni realizar operaciones, pero sus reservas MUST conservarse en el historial.

#### Scenario: Administrador desactiva un usuario
- **WHEN** un administrador desactiva a un usuario
- **THEN** el sistema marca el usuario como inactivo y conserva su historial de reservas

#### Scenario: Reservas pendientes de un usuario desactivado
- **WHEN** un usuario con reservas pendientes es desactivado
- **THEN** el sistema cancela esas reservas pendientes y registra la cancelación en el historial

### Requirement: Matriz de permisos por rol
El sistema MUST validar en el backend los permisos de cada operación según el rol del usuario autenticado, sin depender de la interfaz.

#### Scenario: Operación permitida
- **WHEN** un usuario con rol autorizado para una operación la solicita
- **THEN** el sistema ejecuta la operación

#### Scenario: Operación no permitida
- **WHEN** un usuario sin rol autorizado solicita una operación restringida
- **THEN** el sistema rechaza la solicitud con un error 403 y no modifica datos

#### Scenario: Petición sin autenticación
- **WHEN** una petición a una operación protegida no incluye un token válido
- **THEN** el sistema rechaza la solicitud con un error 401
