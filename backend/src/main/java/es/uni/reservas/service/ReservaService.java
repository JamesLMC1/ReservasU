package es.uni.reservas.service;

import es.uni.reservas.dto.ReservaDto;
import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.HistorialReserva;
import es.uni.reservas.entity.Reserva;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.EspacioRepository;
import es.uni.reservas.repository.HistorialReservaRepository;
import es.uni.reservas.repository.ReservaRepository;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.UsuarioAutenticado;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class ReservaService {

    static final Duration DURACION_MAXIMA = Duration.ofHours(4);
    private static final Collection<EstadoReserva> ESTADOS_ACTIVOS = List.of(EstadoReserva.PENDIENTE, EstadoReserva.APROBADA);

    private final ReservaRepository reservas;
    private final EspacioRepository espacios;
    private final UsuarioRepository usuarios;
    private final HistorialReservaRepository historial;
    private final Clock clock;

    public ReservaService(ReservaRepository reservas, EspacioRepository espacios, UsuarioRepository usuarios,
                          HistorialReservaRepository historial, Clock clock) {
        this.reservas = reservas;
        this.espacios = espacios;
        this.usuarios = usuarios;
        this.historial = historial;
        this.clock = clock;
    }

    @Transactional
    public ReservaDto.Respuesta crear(UsuarioAutenticado actor, ReservaDto.CrearSolicitud solicitud) {
        ZoneId zona = clock.getZone();
        validarBloque(solicitud.inicio(), "inicio");
        validarBloque(solicitud.fin(), "fin");
        if (!solicitud.fin().isAfter(solicitud.inicio())) {
            throw new DominioException(HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior a la de inicio");
        }
        Instant inicio = solicitud.inicio().atZone(zona).toInstant();
        Instant fin = solicitud.fin().atZone(zona).toInstant();
        if (inicio.isBefore(clock.instant())) {
            throw new DominioException(HttpStatus.BAD_REQUEST, "No se pueden reservar fechas pasadas");
        }
        if (Duration.between(inicio, fin).compareTo(DURACION_MAXIMA) > 0) {
            throw new DominioException(HttpStatus.BAD_REQUEST, "Una reserva no puede durar más de cuatro horas consecutivas");
        }

        Espacio espacio = espacios.findByIdParaActualizar(solicitud.espacioId())
                .orElseThrow(() -> new DominioException(HttpStatus.NOT_FOUND, "Espacio no encontrado"));
        if (!espacio.isActivo()) {
            throw new DominioException(HttpStatus.BAD_REQUEST, "El espacio está desactivado");
        }
        if (reservas.contarSolapes(espacio.getId(), ESTADOS_ACTIVOS, inicio, fin) > 0) {
            throw new DominioException(HttpStatus.CONFLICT, "El espacio ya está reservado en ese horario");
        }

        Usuario usuario = usuario(actor);
        Reserva reserva;
        try {
            reserva = reservas.saveAndFlush(new Reserva(usuario, espacio, inicio, fin, clock.instant()));
        } catch (DataIntegrityViolationException ex) {
            throw new DominioException(HttpStatus.CONFLICT, "El espacio ya está reservado en ese horario");
        }
        registrar(reserva, null, EstadoReserva.PENDIENTE, usuario, "Solicitud creada");
        return ReservaDto.Respuesta.de(reserva, zona);
    }

    @Transactional(readOnly = true)
    public List<ReservaDto.Respuesta> listar(UsuarioAutenticado actor, Optional<EstadoReserva> estado) {
        ZoneId zona = clock.getZone();
        List<Reserva> resultado;
        if (actor.rol() == Rol.SOLICITANTE) {
            resultado = reservas.findByUsuarioIdOrderByInicioDesc(actor.id());
        } else if (estado.isPresent()) {
            resultado = reservas.findByEstadoOrderByInicioAsc(estado.get());
        } else {
            resultado = reservas.findAllByOrderByInicioDesc();
        }
        if (estado.isPresent() && actor.rol() == Rol.SOLICITANTE) {
            resultado = resultado.stream().filter(r -> r.getEstado() == estado.get()).toList();
        }
        return resultado.stream().map(r -> ReservaDto.Respuesta.de(r, zona)).toList();
    }

    @Transactional(readOnly = true)
    public ReservaDto.Respuesta obtener(UsuarioAutenticado actor, Long id) {
        Reserva reserva = reserva(id);
        verificarVisibilidad(actor, reserva);
        return ReservaDto.Respuesta.de(reserva, clock.getZone());
    }

    @Transactional
    public ReservaDto.Respuesta cancelar(UsuarioAutenticado actor, Long id) {
        Reserva reserva = bloquearReserva(id);
        verificarVisibilidad(actor, reserva);
        if (!reserva.getEstado().ocupaEspacio()) {
            throw new DominioException(HttpStatus.BAD_REQUEST, "La reserva ya está cerrada");
        }
        EstadoReserva anterior = reserva.getEstado();
        reserva.cambiarEstado(EstadoReserva.CANCELADA);
        registrar(reserva, anterior, EstadoReserva.CANCELADA, usuario(actor), "Reserva cancelada");
        return ReservaDto.Respuesta.de(reserva, clock.getZone());
    }

    @Transactional
    public ReservaDto.Respuesta aprobar(UsuarioAutenticado actor, Long id) {
        verificarAprobador(actor);
        Reserva reserva = bloquearReserva(id);
        verificarNoEsPropia(actor, reserva);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new DominioException(HttpStatus.CONFLICT, "La solicitud ya no está pendiente");
        }
        long solapes = reservas.contarSolapesExcepto(reserva.getEspacio().getId(), ESTADOS_ACTIVOS,
                reserva.getInicio(), reserva.getFin(), reserva.getId());
        if (solapes > 0) {
            throw new DominioException(HttpStatus.CONFLICT, "El espacio ya está reservado en ese horario");
        }
        reserva.cambiarEstado(EstadoReserva.APROBADA);
        registrar(reserva, EstadoReserva.PENDIENTE, EstadoReserva.APROBADA, usuario(actor), "Solicitud aprobada");
        return ReservaDto.Respuesta.de(reserva, clock.getZone());
    }

    @Transactional
    public ReservaDto.Respuesta rechazar(UsuarioAutenticado actor, Long id, String motivo) {
        verificarAprobador(actor);
        if (motivo == null || motivo.isBlank()) {
            throw new DominioException(HttpStatus.BAD_REQUEST, "El motivo de rechazo es obligatorio");
        }
        Reserva reserva = bloquearReserva(id);
        verificarNoEsPropia(actor, reserva);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new DominioException(HttpStatus.CONFLICT, "La solicitud ya no está pendiente");
        }
        reserva.rechazar(motivo.trim());
        registrar(reserva, EstadoReserva.PENDIENTE, EstadoReserva.RECHAZADA, usuario(actor), motivo.trim());
        return ReservaDto.Respuesta.de(reserva, clock.getZone());
    }

    @Transactional(readOnly = true)
    public List<ReservaDto.HistorialRespuesta> historial(UsuarioAutenticado actor, Long id) {
        Reserva reserva = reserva(id);
        verificarVisibilidad(actor, reserva);
        ZoneId zona = clock.getZone();
        return historial.findByReservaIdOrderByFechaAsc(id).stream()
                .map(h -> ReservaDto.HistorialRespuesta.de(h, zona))
                .toList();
    }

    private void validarBloque(LocalDateTime momento, String campo) {
        if (momento.getSecond() != 0 || momento.getNano() != 0 || momento.getMinute() % 30 != 0) {
            throw new DominioException(HttpStatus.BAD_REQUEST,
                    "La hora de " + campo + " debe ser un múltiplo de 30 minutos");
        }
    }

    private Reserva reserva(Long id) {
        return reservas.findById(id)
                .orElseThrow(() -> new DominioException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }

    /**
     * Bloquea primero el espacio y luego la reserva, siempre en ese orden, para evitar interbloqueos
     * entre operaciones concurrentes sobre el mismo espacio.
     */
    private Reserva bloquearReserva(Long id) {
        Long espacioId = reservas.findEspacioIdPorReserva(id)
                .orElseThrow(() -> new DominioException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        espacios.findByIdParaActualizar(espacioId);
        return reservas.findByIdParaActualizar(id)
                .orElseThrow(() -> new DominioException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }

    private void verificarVisibilidad(UsuarioAutenticado actor, Reserva reserva) {
        if (actor.rol() == Rol.SOLICITANTE && !reserva.getUsuario().getId().equals(actor.id())) {
            throw new DominioException(HttpStatus.FORBIDDEN, "No puede acceder a reservas de otros usuarios");
        }
    }

    private void verificarAprobador(UsuarioAutenticado actor) {
        if (actor.rol() == Rol.SOLICITANTE) {
            throw new DominioException(HttpStatus.FORBIDDEN, "Solo un aprobador o administrador puede decidir");
        }
    }

    private void verificarNoEsPropia(UsuarioAutenticado actor, Reserva reserva) {
        if (reserva.getUsuario().getId().equals(actor.id())) {
            throw new DominioException(HttpStatus.FORBIDDEN, "No puede decidir sobre una solicitud propia");
        }
    }

    private Usuario usuario(UsuarioAutenticado actor) {
        return usuarios.findById(actor.id())
                .orElseThrow(() -> new DominioException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private void registrar(Reserva reserva, EstadoReserva anterior, EstadoReserva nuevo, Usuario responsable,
                           String detalle) {
        historial.save(new HistorialReserva(reserva, anterior, nuevo, responsable, clock.instant(), detalle));
    }
}
