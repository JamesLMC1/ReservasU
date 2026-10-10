package es.uni.reservas.dto;

import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.HistorialReserva;
import es.uni.reservas.entity.Reserva;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class ReservaDto {

    private ReservaDto() {
    }

    public record CrearSolicitud(
            @NotNull Long espacioId,
            @NotNull LocalDateTime inicio,
            @NotNull LocalDateTime fin) {
    }

    public record RechazoSolicitud(@NotBlank @Size(max = 500) String motivo) {
    }

    public record Respuesta(
            Long id,
            Long espacioId,
            String espacioNombre,
            Long usuarioId,
            String usuarioCorreo,
            OffsetDateTime inicio,
            OffsetDateTime fin,
            EstadoReserva estado,
            String motivoRechazo) {

        public static Respuesta de(Reserva reserva, ZoneId zona) {
            return new Respuesta(
                    reserva.getId(),
                    reserva.getEspacio().getId(),
                    reserva.getEspacio().getNombre(),
                    reserva.getUsuario().getId(),
                    reserva.getUsuario().getCorreo(),
                    reserva.getInicio().atZone(zona).toOffsetDateTime(),
                    reserva.getFin().atZone(zona).toOffsetDateTime(),
                    reserva.getEstado(),
                    reserva.getMotivoRechazo());
        }
    }

    public record HistorialRespuesta(
            EstadoReserva estadoAnterior,
            EstadoReserva estadoNuevo,
            Long usuarioId,
            String usuarioCorreo,
            OffsetDateTime fecha,
            String detalle) {

        public static HistorialRespuesta de(HistorialReserva historial, ZoneId zona) {
            return new HistorialRespuesta(
                    historial.getEstadoAnterior(),
                    historial.getEstadoNuevo(),
                    historial.getUsuario().getId(),
                    historial.getUsuario().getCorreo(),
                    historial.getFecha().atZone(zona).toOffsetDateTime(),
                    historial.getDetalle());
        }
    }
}
