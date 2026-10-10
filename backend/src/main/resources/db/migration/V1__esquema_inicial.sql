CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuario (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    correo          VARCHAR(190) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'APROBADOR', 'SOLICITANTE')),
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE espacio (
    id      BIGSERIAL PRIMARY KEY,
    nombre  VARCHAR(120) NOT NULL UNIQUE,
    tipo    VARCHAR(30)  NOT NULL CHECK (tipo IN ('LABORATORIO', 'AUDITORIO', 'SALA_COMPUTO')),
    activo  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE reserva (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL REFERENCES usuario (id),
    espacio_id      BIGINT       NOT NULL REFERENCES espacio (id),
    inicio          TIMESTAMPTZ  NOT NULL,
    fin             TIMESTAMPTZ  NOT NULL,
    estado          VARCHAR(20)  NOT NULL CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'CANCELADA')),
    motivo_rechazo  VARCHAR(500),
    creada_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_reserva_orden CHECK (fin > inicio),
    CONSTRAINT chk_reserva_duracion CHECK (fin - inicio <= INTERVAL '4 hours'),
    CONSTRAINT no_solapamiento_reservas EXCLUDE USING gist (
        espacio_id WITH =,
        tstzrange(inicio, fin, '[)') WITH &&
    ) WHERE (estado IN ('PENDIENTE', 'APROBADA'))
);

CREATE INDEX idx_reserva_usuario ON reserva (usuario_id);

CREATE TABLE historial_reserva (
    id               BIGSERIAL PRIMARY KEY,
    reserva_id       BIGINT       NOT NULL REFERENCES reserva (id),
    estado_anterior  VARCHAR(20)  CHECK (estado_anterior IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'CANCELADA')),
    estado_nuevo     VARCHAR(20)  NOT NULL CHECK (estado_nuevo IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'CANCELADA')),
    usuario_id       BIGINT       NOT NULL REFERENCES usuario (id),
    fecha            TIMESTAMPTZ  NOT NULL,
    detalle          VARCHAR(500)
);

CREATE INDEX idx_historial_reserva ON historial_reserva (reserva_id);
