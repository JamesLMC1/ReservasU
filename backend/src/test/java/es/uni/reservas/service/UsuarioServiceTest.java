package es.uni.reservas.service;

import es.uni.reservas.dto.UsuarioDto;
import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.HistorialReserva;
import es.uni.reservas.entity.Reserva;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.HistorialReservaRepository;
import es.uni.reservas.repository.ReservaRepository;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static es.uni.reservas.DatosPrueba.espacio;
import static es.uni.reservas.DatosPrueba.reloj;
import static es.uni.reservas.DatosPrueba.reserva;
import static es.uni.reservas.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private ReservaRepository reservas;
    @Mock
    private HistorialReservaRepository historial;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarios, reservas, historial, passwordEncoder, reloj());
    }

    @Test
    @DisplayName("No permite crear dos usuarios con el mismo correo")
    void rechazaCorreoDuplicado() {
        when(usuarios.existsByCorreo("ana@uni.es")).thenReturn(true);
        UsuarioDto.CrearSolicitud solicitud = new UsuarioDto.CrearSolicitud(
                "Ana", "ana@uni.es", "clave-segura-1", Rol.SOLICITANTE);

        DominioException ex = assertThrows(DominioException.class, () -> servicio.crear(solicitud));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    @DisplayName("Cifra la contraseña antes de guardar el usuario")
    void cifraContrasena() {
        when(usuarios.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode("clave-segura-1")).thenReturn("hash-cifrado");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        UsuarioDto.CrearSolicitud solicitud = new UsuarioDto.CrearSolicitud(
                "Ana", "ana@uni.es", "clave-segura-1", Rol.APROBADOR);

        UsuarioDto.Respuesta respuesta = servicio.crear(solicitud);

        verify(passwordEncoder).encode("clave-segura-1");
        assertEquals(Rol.APROBADOR, respuesta.rol());
    }

    @Test
    @DisplayName("Al desactivar, cancela las pendientes y no toca las aprobadas")
    void desactivaCancelandoPendientes() {
        Usuario desactivado = usuario(5L, Rol.SOLICITANTE);
        Usuario admin = usuario(1L, Rol.ADMINISTRADOR);
        Reserva pendiente = reserva(10L, desactivado, espacio(1L), EstadoReserva.PENDIENTE);
        when(usuarios.findById(5L)).thenReturn(Optional.of(desactivado));
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        when(reservas.findByUsuarioIdAndEstado(5L, EstadoReserva.PENDIENTE)).thenReturn(List.of(pendiente));

        UsuarioDto.Respuesta respuesta = servicio.desactivar(new UsuarioAutenticado(1L, "admin@uni.es",
                Rol.ADMINISTRADOR), 5L);

        assertEquals(false, respuesta.activo());
        assertEquals(EstadoReserva.CANCELADA, pendiente.getEstado());
        verify(historial).save(any(HistorialReserva.class));
    }
}
