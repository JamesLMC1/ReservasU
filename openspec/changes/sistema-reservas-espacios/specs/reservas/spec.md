# Especificación delta

## Purpose

Define cómo se solicitan, consultan, cancelan y auditan las reservas de espacios académicos, incluida la validación de disponibilidad y la duración máxima.

## ADDED Requirements

### Requirement: Creación de solicitud de reserva
El sistema SHALL permitir a los usuarios autenticados crear una solicitud de reserva indicando espacio, fecha, hora de inicio y hora de fin. Toda solicitud nueva MUST quedar en estado PENDIENTE.

#### Scenario: Solicitud válida
- **WHEN** un usuario solicita un espacio activo en un intervalo futuro, libre y válido
- **THEN** el sistema crea la reserva en estado PENDIENTE y la devuelve con su identificador

#### Scenario: Fin anterior o igual al inicio
- **WHEN** un usuario indica una hora de fin que no es posterior a la de inicio
- **THEN** el sistema rechaza la solicitud con un error de validación

#### Scenario: Inicio en el pasado
- **WHEN** un usuario solicita una reserva cuyo inicio ya ha pasado
- **THEN** el sistema rechaza la solicitud con un error de validación

#### Scenario: Horas no alineadas a bloques de 30 minutos
- **WHEN** un usuario indica un inicio o un fin que no cae en un múltiplo de 30 minutos
- **THEN** el sistema rechaza la solicitud con un error de validación

### Requirement: Duración máxima de cuatro horas por reserva
El sistema MUST rechazar cualquier solicitud de reserva cuya duración sea superior a cuatro horas consecutivas. La regla se aplica a cada reserva individual.

#### Scenario: Reserva de exactamente cuatro horas
- **WHEN** un usuario solicita una reserva de 10:00 a 14:00 en un espacio libre
- **THEN** el sistema acepta la solicitud

#### Scenario: Reserva de más de cuatro horas
- **WHEN** un usuario solicita una reserva de 10:00 a 14:30
- **THEN** el sistema rechaza la solicitud con un error de validación y no crea la reserva

#### Scenario: Reservas contiguas del mismo usuario
- **WHEN** un usuario ya tiene una reserva de 10:00 a 14:00 en un espacio y solicita otra de 14:00 a 18:00 en el mismo espacio
- **THEN** el sistema acepta la segunda solicitud porque la regla se aplica por reserva individual. Esta situación queda como pregunta abierta en el diseño

### Requirement: Prevención de solapamientos
El sistema MUST impedir que dos reservas en estado PENDIENTE o APROBADA ocupen el mismo espacio en intervalos que se solapen. Los intervalos son semiabiertos: una reserva que termina a las 12:00 no solapa con una que empieza a las 12:00.

#### Scenario: Intervalo que se solapa con una reserva activa
- **WHEN** un usuario solicita un espacio con un intervalo que se solapa con una reserva PENDIENTE o APROBADA del mismo espacio
- **THEN** el sistema rechaza la solicitud con un error de conflicto (409)

#### Scenario: Intervalo contiguo sin solapamiento
- **WHEN** un usuario solicita un espacio desde las 12:00, y existe una reserva activa que termina a las 12:00
- **THEN** el sistema acepta la solicitud

#### Scenario: Reserva cancelada o rechazada no bloquea
- **WHEN** un usuario solicita un intervalo ocupado únicamente por una reserva RECHAZADA o CANCELADA
- **THEN** el sistema acepta la solicitud

#### Scenario: Mismo intervalo en espacios distintos
- **WHEN** un usuario solicita un intervalo en un espacio, y existe una reserva activa del mismo intervalo en otro espacio
- **THEN** el sistema acepta la solicitud

### Requirement: Consistencia ante solicitudes simultáneas
El sistema MUST garantizar que, si varias solicitudes simultáneas pretenden ocupar el mismo espacio en intervalos solapados, como máximo una quede activa. Las demás MUST recibir un error de conflicto (409).

#### Scenario: Dos solicitudes simultáneas para el mismo intervalo
- **WHEN** dos usuarios envían al mismo tiempo solicitudes solapadas para el mismo espacio
- **THEN** exactamente una solicitud se registra y la otra recibe un error de conflicto (409)

#### Scenario: Solicitud que pasa la validación previa pero choca en base de datos
- **WHEN** la validación previa acepta una solicitud y, antes de guardarla, otra transacción confirma una reserva solapada
- **THEN** el sistema rechaza la solicitud con un error de conflicto (409) y no deja la base de datos en estado inconsistente

### Requirement: Consulta de reservas
El sistema SHALL permitir a los solicitantes consultar solo sus propias reservas. Los aprobadores y administradores SHALL poder consultar todas las reservas.

#### Scenario: Solicitante consulta sus reservas
- **WHEN** un solicitante consulta el listado de reservas
- **THEN** el sistema devuelve solo las reservas creadas por ese solicitante

#### Scenario: Aprobador consulta todas las reservas
- **WHEN** un aprobador consulta el listado de reservas
- **THEN** el sistema devuelve las reservas de todos los usuarios

#### Scenario: Solicitante intenta ver la reserva de otro usuario
- **WHEN** un solicitante pide el detalle de una reserva creada por otro usuario
- **THEN** el sistema responde con error 403

### Requirement: Cancelación de reservas
El sistema SHALL permitir cancelar una reserva en estado PENDIENTE o APROBADA. Un solicitante MUST poder cancelar solo sus propias reservas. Un aprobador o administrador MUST poder cancelar cualquier reserva.

#### Scenario: Solicitante cancela su reserva
- **WHEN** un solicitante cancela una de sus reservas PENDIENTE o APROBADA
- **THEN** el sistema cambia el estado a CANCELADA y libera el intervalo

#### Scenario: Solicitante intenta cancelar la reserva de otro usuario
- **WHEN** un solicitante intenta cancelar una reserva creada por otro usuario
- **THEN** el sistema rechaza la operación con un error 403

#### Scenario: Cancelación de una reserva ya cerrada
- **WHEN** alguien intenta cancelar una reserva en estado RECHAZADA o CANCELADA
- **THEN** el sistema rechaza la operación con un error de validación

### Requirement: Historial de cambios de estado
El sistema MUST registrar cada cambio de estado de una reserva con el estado anterior, el nuevo estado, el usuario responsable, la fecha y un detalle opcional. El registro MUST hacerse en la misma transacción que el cambio.

#### Scenario: Consulta del historial
- **WHEN** el propietario de una reserva, o un aprobador o administrador, consulta su historial
- **THEN** el sistema devuelve todos sus cambios de estado en orden cronológico

#### Scenario: Historial de reserva ajena por un solicitante
- **WHEN** un solicitante consulta el historial de una reserva que no es suya
- **THEN** el sistema responde con error 403

#### Scenario: Fallo al registrar el historial
- **WHEN** falla el registro del historial durante un cambio de estado
- **THEN** el sistema revierte el cambio de estado y no lo deja aplicado
