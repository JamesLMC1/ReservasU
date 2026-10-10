package es.uni.reservas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "historial_reserva")
public class HistorialReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoReserva estadoNuevo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Instant fecha;

    @Column(length = 500)
    private String detalle;

    protected HistorialReserva() {
    }

    public HistorialReserva(Reserva reserva, EstadoReserva estadoAnterior, EstadoReserva estadoNuevo,
                            Usuario usuario, Instant fecha, String detalle) {
        this.reserva = reserva;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
        this.fecha = fecha;
        this.detalle = detalle;
    }

    public Long getId() {
        return id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public EstadoReserva getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoReserva getEstadoNuevo() {
        return estadoNuevo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getDetalle() {
        return detalle;
    }
}
