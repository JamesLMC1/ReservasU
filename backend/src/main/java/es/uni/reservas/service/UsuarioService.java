package es.uni.reservas.service;

import es.uni.reservas.dto.UsuarioDto;
import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.HistorialReserva;
import es.uni.reservas.entity.Reserva;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.HistorialReservaRepository;
import es.uni.reservas.repository.ReservaRepository;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final HistorialReservaRepository historial;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarios, ReservaRepository reservas,
                          HistorialReservaRepository historial, PasswordEncoder passwordEncoder, Clock clock) {
        this.usuarios = usuarios;
        this.reservas = reservas;
        this.historial = historial;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public UsuarioDto.Respuesta crear(UsuarioDto.CrearSolicitud solicitud) {
        if (usuarios.existsByCorreo(solicitud.correo())) {
            throw new DominioException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        Usuario usuario = new Usuario(solicitud.nombre(), solicitud.correo(),
                passwordEncoder.encode(solicitud.contrasena()), solicitud.rol());
        return UsuarioDto.Respuesta.de(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto.Respuesta> listar() {
        return usuarios.findAllByOrderByNombreAsc().stream().map(UsuarioDto.Respuesta::de).toList();
    }

    @Transactional
    public UsuarioDto.Respuesta cambiarRol(Long id, UsuarioDto.CambiarRolSolicitud solicitud) {
        Usuario usuario = buscar(id);
        usuario.cambiarRol(solicitud.rol());
        return UsuarioDto.Respuesta.de(usuario);
    }

    /**
     * Desactiva al usuario e impide que sus solicitudes pendientes sigan ocupando espacios.
     * Las reservas aprobadas se mantienen y el historial se conserva.
     */
    @Transactional
    public UsuarioDto.Respuesta desactivar(UsuarioAutenticado actor, Long id) {
        Usuario usuario = buscar(id);
        usuario.desactivar();
        Usuario responsable = usuarios.findById(actor.id())
                .orElseThrow(() -> new DominioException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        for (Reserva reserva : reservas.findByUsuarioIdAndEstado(id, EstadoReserva.PENDIENTE)) {
            reserva.cambiarEstado(EstadoReserva.CANCELADA);
            historial.save(new HistorialReserva(reserva, EstadoReserva.PENDIENTE, EstadoReserva.CANCELADA,
                    responsable, clock.instant(), "Cancelada por desactivación del usuario"));
        }
        return UsuarioDto.Respuesta.de(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new DominioException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
