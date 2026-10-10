# Especificación delta

## Purpose

Define los espacios académicos reservables, sus tipos y la forma en que se dan de alta, modifican y desactivan.

## ADDED Requirements

### Requirement: Tipos de espacio
El sistema SHALL clasificar cada espacio académico en exactamente uno de estos tipos: laboratorio, auditorio o sala de cómputo.

#### Scenario: Espacio con tipo válido
- **WHEN** un administrador crea un espacio de tipo "auditorio"
- **THEN** el sistema guarda el espacio con ese tipo

#### Scenario: Tipo no válido
- **WHEN** un administrador intenta crear un espacio con un tipo fuera de la lista
- **THEN** el sistema rechaza la operación con un error de validación

### Requirement: Alta de espacios
El sistema SHALL permitir a los administradores registrar espacios con nombre único, tipo y estado activo.

#### Scenario: Alta correcta
- **WHEN** un administrador registra un espacio con nombre único y tipo válido
- **THEN** el sistema crea el espacio en estado activo

#### Scenario: Nombre duplicado
- **WHEN** un administrador registra un espacio con un nombre que ya existe
- **THEN** el sistema rechaza la operación con un error de conflicto (409)

### Requirement: Modificación de espacios
El sistema SHALL permitir a los administradores modificar el nombre y el tipo de un espacio existente.

#### Scenario: Modificación de un espacio sin reservas activas
- **WHEN** un administrador cambia el nombre o el tipo de un espacio
- **THEN** el sistema guarda los cambios

#### Scenario: Modificación de un espacio inexistente
- **WHEN** un administrador modifica un espacio que no existe
- **THEN** el sistema responde con error 404

### Requirement: Desactivación de espacios
El sistema SHALL permitir a los administradores desactivar un espacio. Un espacio desactivado MUST NOT aceptar nuevas solicitudes de reserva.

#### Scenario: Desactivación de un espacio
- **WHEN** un administrador desactiva un espacio
- **THEN** el sistema marca el espacio como inactivo y deja de aceptar nuevas solicitudes sobre él

#### Scenario: Solicitud sobre un espacio desactivado
- **WHEN** un usuario intenta crear una reserva sobre un espacio inactivo
- **THEN** el sistema rechaza la solicitud con un error de validación

### Requirement: Consulta de espacios
El sistema SHALL permitir a cualquier usuario autenticado consultar los espacios activos, con su nombre y tipo.

#### Scenario: Listado de espacios
- **WHEN** un usuario autenticado solicita el listado de espacios
- **THEN** el sistema devuelve solo los espacios activos con nombre y tipo

#### Scenario: Acceso sin autenticación
- **WHEN** una petición de listado de espacios no incluye token válido
- **THEN** el sistema rechaza la solicitud con un error 401
