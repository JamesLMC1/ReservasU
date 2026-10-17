package es.uni.reservas.integracion;

import es.uni.reservas.dto.UsuarioDto;
import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.Reserva;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.exception.DominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class UsuariosIntegracionTest extends SoporteIntegracion {

    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    @Test
    @DisplayName("El servicio no permite dos usuarios con el mismo correo")
    void correoDuplicadoEnServicio() {
        String correo = correoUnico();
        usuarioService.crear(new UsuarioDto.CrearSolicitud("Ana", correo, "clave-segura-1", Rol.SOLICITANTE));

        DominioException ex = assertThrows(DominioException.class,
                () -> usuarioService.crear(new UsuarioDto.CrearSolicitud("Otra", correo, "clave-segura-1", Rol.SOLICITANTE)));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    @DisplayName("La base de datos rechaza el correo duplicado aunque se inserte directamente")
    void correoDuplicadoEnBaseDeDatos() {
        String correo = correoUnico();
        usuariosRepo.saveAndFlush(new Usuario("Ana", correo, "hash", Rol.SOLICITANTE));

        assertThrows(DataIntegrityViolationException.class,
                () -> usuariosRepo.saveAndFlush(new Usuario("Otra", correo, "hash", Rol.SOLICITANTE)));
    }

    @Test
    @DisplayName("Al desactivar un usuario, sus pendientes pasan a CANCELADA y las aprobadas se mantienen")
    void desactivarCancelaPendientesYMantieneAprobadas() {
        Usuario admin = crearUsuario(Rol.ADMINISTRADOR);
        Usuario objetivo = crearUsuario(Rol.SOLICITANTE);
        Espacio espacio = crearEspacio();
        Reserva pendiente = guardarReserva(objetivo, espacio, futuro(10, 8, 0), futuro(10, 10, 0), EstadoReserva.PENDIENTE);
        Reserva aprobada = guardarReserva(objetivo, espacio, futuro(10, 12, 0), futuro(10, 14, 0), EstadoReserva.APROBADA);

        usuarioService.desactivar(actor(admin), objetivo.getId());

        assertEquals(EstadoReserva.CANCELADA, reservasRepo.findById(pendiente.getId()).orElseThrow().getEstado());
        assertEquals(EstadoReserva.APROBADA, reservasRepo.findById(aprobada.getId()).orElseThrow().getEstado());
    }

    private Reserva guardarReserva(Usuario usuario, Espacio espacio, LocalDateTime inicio, LocalDateTime fin,
                                   EstadoReserva estado) {
        Reserva reserva = new Reserva(usuario, espacio, inicio.atZone(ZONA).toInstant(), fin.atZone(ZONA).toInstant(),
                Instant.now());
        if (estado != EstadoReserva.PENDIENTE) {
            reserva.cambiarEstado(estado);
        }
        return reservasRepo.saveAndFlush(reserva);
    }
}
