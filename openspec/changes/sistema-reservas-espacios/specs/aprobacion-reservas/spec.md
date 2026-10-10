# Especificación delta

## Purpose

Define cómo los aprobadores y administradores aprueban o rechazan solicitudes de reserva pendientes.

## ADDED Requirements

### Requirement: Quién puede aprobar o rechazar
El sistema SHALL permitir aprobar o rechazar solicitudes solo a usuarios con rol aprobador o administrador. Un usuario MUST NOT aprobar ni rechazar una solicitud creada por sí mismo.

#### Scenario: Aprobador aprueba una solicitud ajena
- **WHEN** un aprobador aprueba una solicitud PENDIENTE creada por otro usuario
- **THEN** el sistema cambia la reserva a APROBADA

#### Scenario: Solicitante intenta aprobar
- **WHEN** un solicitante intenta aprobar una solicitud
- **THEN** el sistema rechaza la operación con un error 403

#### Scenario: Aprobador intenta aprobar su propia solicitud
- **WHEN** un aprobador intenta aprobar una reserva que él mismo creó
- **THEN** el sistema rechaza la operación con un error 403

### Requirement: Aprobación de solicitudes pendientes
El sistema SHALL aprobar únicamente solicitudes en estado PENDIENTE. Al aprobarse, la reserva MUST seguir ocupando el intervalo sin cambios.

#### Scenario: Aprobación de solicitud pendiente
- **WHEN** un aprobador aprueba una solicitud PENDIENTE
- **THEN** el sistema cambia el estado a APROBADA y registra el cambio en el historial

#### Scenario: Aprobación de solicitud no pendiente
- **WHEN** un aprobador intenta aprobar una reserva que está APROBADA, RECHAZADA o CANCELADA
- **THEN** el sistema rechaza la operación con un error de validación

### Requirement: Rechazo con motivo obligatorio
El sistema SHALL rechazar solicitudes PENDIENTE solo si se indica un motivo de rechazo no vacío. Al rechazarse, la reserva MUST liberar el intervalo.

#### Scenario: Rechazo con motivo
- **WHEN** un aprobador rechaza una solicitud PENDIENTE indicando un motivo
- **THEN** el sistema cambia el estado a RECHAZADA, guarda el motivo y registra el cambio en el historial

#### Scenario: Rechazo sin motivo
- **WHEN** un aprobador rechaza una solicitud sin motivo o con un motivo vacío
- **THEN** el sistema rechaza la operación con un error de validación y la solicitud sigue PENDIENTE

#### Scenario: Rechazo de solicitud no pendiente
- **WHEN** un aprobador intenta rechazar una reserva que ya no está PENDIENTE
- **THEN** el sistema rechaza la operación con un error de validación

### Requirement: Decisiones concurrentes sobre una misma solicitud
El sistema MUST garantizar que una solicitud PENDIENTE sea decidida una sola vez. Si dos aprobadores deciden a la vez, solo una decisión MUST aplicarse y la otra MUST recibir un error de conflicto (409).

#### Scenario: Aprobación y rechazo simultáneos
- **WHEN** dos aprobadores aprueban y rechazan la misma solicitud PENDIENTE al mismo tiempo
- **THEN** solo una de las decisiones se aplica y la otra recibe un error de conflicto (409)

#### Scenario: Aprobación de una solicitud ya decidida
- **WHEN** un aprobador aprueba una solicitud que otro aprobador acaba de rechazar
- **THEN** el sistema responde con error de conflicto (409) y mantiene el estado RECHAZADA
