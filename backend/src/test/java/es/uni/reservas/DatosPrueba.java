package es.uni.reservas;

import es.uni.reservas.dto.ReservaDto;
import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.Reserva;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.TipoEspacio;
import es.uni.reservas.entity.Usuario;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DatosPrueba {

    public static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    private DatosPrueba() {
    }

    /** Reloj fijo: 9 de octubre de 2026, 10:00 en Madrid. */
    public static Clock reloj() {
        return Clock.fixed(Instant.parse("2026-10-09T08:00:00Z"), ZONA);
    }

    public static LocalDateTime fecha(int hora, int minuto) {
        return LocalDateTime.of(2026, 10, 12, hora, minuto);
    }

    public static Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario("Usuario " + id, "usuario" + id + "@uni.es", "hash", rol);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    public static Espacio espacio(Long id) {
        Espacio espacio = new Espacio("Laboratorio " + id, TipoEspacio.LABORATORIO);
        ReflectionTestUtils.setField(espacio, "id", id);
        return espacio;
    }

    public static Reserva reserva(Long id, Usuario dueno, Espacio espacio, EstadoReserva estado) {
        Reserva reserva = new Reserva(dueno, espacio,
                fecha(10, 0).atZone(ZONA).toInstant(),
                fecha(14, 0).atZone(ZONA).toInstant(),
                Instant.parse("2026-10-08T08:00:00Z"));
        ReflectionTestUtils.setField(reserva, "id", id);
        if (estado != EstadoReserva.PENDIENTE) {
            reserva.cambiarEstado(estado);
        }
        return reserva;
    }

    public static ReservaDto.CrearSolicitud solicitud(LocalDateTime inicio, LocalDateTime fin) {
        return new ReservaDto.CrearSolicitud(1L, inicio, fin);
    }
}
