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
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "espacio_id", nullable = false)
    private Espacio espacio;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected Reserva() {
    }

    public Reserva(Usuario usuario, Espacio espacio, Instant inicio, Instant fin, Instant creadaEn) {
        this.usuario = usuario;
        this.espacio = espacio;
        this.inicio = inicio;
        this.fin = fin;
        this.estado = EstadoReserva.PENDIENTE;
        this.creadaEn = creadaEn;
    }

    public void cambiarEstado(EstadoReserva nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public void rechazar(String motivo) {
        this.estado = EstadoReserva.RECHAZADA;
        this.motivoRechazo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Espacio getEspacio() {
        return espacio;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFin() {
        return fin;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }
}
