package es.uni.reservas.integracion;

import es.uni.reservas.dto.ReservaDto;
import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.Reserva;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.security.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ReservasIntegracionTest extends SoporteIntegracion {

    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    @Test
    @DisplayName("La restriccion EXCLUDE rechaza dos reservas solapadas en el mismo espacio")
    void restriccionExcludeRechazaSolapamiento() {
        Usuario usuario = crearUsuario(Rol.SOLICITANTE);
        Espacio espacio = crearEspacio();
        guardarReserva(usuario, espacio, futuro(1, 10, 0), futuro(1, 12, 0), EstadoReserva.PENDIENTE);

        assertThrows(DataIntegrityViolationException.class,
                () -> guardarReserva(usuario, espacio, futuro(1, 11, 0), futuro(1, 13, 0), EstadoReserva.PENDIENTE));
    }

    @Test
    @DisplayName("La restriccion EXCLUDE permite intervalos contiguos")
    void restriccionExcludePermiteContiguos() {
        Usuario usuario = crearUsuario(Rol.SOLICITANTE);
        Espacio espacio = crearEspacio();
        guardarReserva(usuario, espacio, futuro(2, 10, 0), futuro(2, 12, 0), EstadoReserva.PENDIENTE);

        Reserva contigua = guardarReserva(usuario, espacio, futuro(2, 12, 0), futuro(2, 14, 0), EstadoReserva.PENDIENTE);

        assertEquals(EstadoReserva.PENDIENTE, contigua.getEstado());
    }

    @Test
    @DisplayName("La restriccion CHECK de la base de datos rechaza una reserva de cuatro horas y media")
    void restriccionCheckRechazaDuracionExcesiva() {
        Usuario usuario = crearUsuario(Rol.SOLICITANTE);
        Espacio espacio = crearEspacio();

        assertThrows(DataIntegrityViolationException.class,
                () -> guardarReserva(usuario, espacio, futuro(3, 10, 0), futuro(3, 14, 30), EstadoReserva.PENDIENTE));
    }

    @Test
    @DisplayName("El servicio rechaza un solapamiento con una reserva activa del mismo espacio")
    void servicioRechazaSolapamiento() {
        Usuario usuario = crearUsuario(Rol.SOLICITANTE);
        Espacio espacio = crearEspacio();
        guardarReserva(usuario, espacio, futuro(4, 10, 0), futuro(4, 12, 0), EstadoReserva.APROBADA);

        DominioException ex = assertThrows(DominioException.class,
                () -> reservas.crear(actor(usuario), solicitud(espacio, futuro(4, 11, 0), futuro(4, 13, 0))));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    @DisplayName("Una reserva cancelada o rechazada no bloquea el mismo intervalo")
    void reservaCerradaNoBloquea() {
        Usuario usuario = crearUsuario(Rol.SOLICITANTE);
        Espacio espacio = crearEspacio();
        guardarReserva(usuario, espacio, futuro(5, 10, 0), futuro(5, 12, 0), EstadoReserva.CANCELADA);
        guardarReserva(usuario, espacio, futuro(5, 10, 0), futuro(5, 12, 0), EstadoReserva.RECHAZADA);

        ReservaDto.Respuesta respuesta = reservas.crear(actor(usuario), solicitud(espacio, futuro(5, 10, 0), futuro(5, 12, 0)));

        assertEquals(EstadoReserva.PENDIENTE, respuesta.estado());
    }

    @Test
    @DisplayName("El mismo intervalo en espacios distintos no bloquea")
    void mismoIntervaloOtroEspacioNoBloquea() {
        Usuario usuario = crearUsuario(Rol.SOLICITANTE);
        Espacio primero = crearEspacio();
        Espacio segundo = crearEspacio();
        guardarReserva(usuario, primero, futuro(6, 10, 0), futuro(6, 12, 0), EstadoReserva.APROBADA);

        ReservaDto.Respuesta respuesta = reservas.crear(actor(usuario), solicitud(segundo, futuro(6, 10, 0), futuro(6, 12, 0)));

        assertEquals(EstadoReserva.PENDIENTE, respuesta.estado());
    }

    @Test
    @DisplayName("El servicio rechaza una reserva de mas de cuatro horas")
    void servicioRechazaDuracionExcesiva() {
        Usuario usuario = crearUsuario(Rol.SOLICITANTE);
        Espacio espacio = crearEspacio();

        DominioException ex = assertThrows(DominioException.class,
                () -> reservas.crear(actor(usuario), solicitud(espacio, futuro(7, 10, 0), futuro(7, 14, 30))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
    }

    @Test
    @DisplayName("Dos solicitudes simultaneas para el mismo espacio e intervalo: solo una queda registrada")
    void solicitudesSimultaneasSoloUnaQueda() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int intento = 0; intento < 20; intento++) {
                Usuario usuario = crearUsuario(Rol.SOLICITANTE);
                Espacio espacio = crearEspacio();
                UsuarioAutenticado actor = actor(usuario);
                ReservaDto.CrearSolicitud peticion = solicitud(espacio, futuro(8, 10, 0), futuro(8, 12, 0));

                CyclicBarrier barrera = new CyclicBarrier(2);
                List<Future<String>> resultados = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    resultados.add(pool.submit(intentar(actor, peticion, barrera)));
                }

                int exitos = 0;
                int conflictos = 0;
                for (Future<String> resultado : resultados) {
                    String estado = resultado.get();
                    if ("OK".equals(estado)) {
                        exitos++;
                    } else if ("409".equals(estado)) {
                        conflictos++;
                    }
                }
                assertEquals(1, exitos, "intento " + intento);
                assertEquals(1, conflictos, "intento " + intento);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<String> intentar(UsuarioAutenticado actor, ReservaDto.CrearSolicitud peticion, CyclicBarrier barrera) {
        return () -> {
            barrera.await();
            try {
                reservas.crear(actor, peticion);
                return "OK";
            } catch (DominioException ex) {
                return ex.getEstado() == HttpStatus.CONFLICT ? "409" : ex.getEstado().name();
            }
        };
    }

    private Reserva guardarReserva(Usuario usuario, Espacio espacio, java.time.LocalDateTime inicio,
                                   java.time.LocalDateTime fin, EstadoReserva estado) {
        Reserva reserva = new Reserva(usuario, espacio, inicio.atZone(ZONA).toInstant(), fin.atZone(ZONA).toInstant(),
                Instant.now());
        if (estado != EstadoReserva.PENDIENTE) {
            reserva.cambiarEstado(estado);
        }
        return reservasRepo.saveAndFlush(reserva);
    }

    private ReservaDto.CrearSolicitud solicitud(Espacio espacio, java.time.LocalDateTime inicio,
                                                java.time.LocalDateTime fin) {
        return new ReservaDto.CrearSolicitud(espacio.getId(), inicio, fin);
    }
}
