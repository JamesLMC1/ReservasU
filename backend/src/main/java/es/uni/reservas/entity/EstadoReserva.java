package es.uni.reservas.entity;

public enum EstadoReserva {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    CANCELADA;

    public boolean ocupaEspacio() {
        return this == PENDIENTE || this == APROBADA;
    }
}
