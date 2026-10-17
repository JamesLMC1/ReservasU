package es.uni.reservas.integracion;

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
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class AprobacionConcurrenteIntegracionTest extends SoporteIntegracion {

    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    @Test
    @DisplayName("Aprobar y rechazar la misma solicitud a la vez: solo una decision se aplica")
    void decisionConcurrenteSoloUnaSeAplica() throws Exception {
        Usuario solicitante = crearUsuario(Rol.SOLICITANTE);
        Usuario aprobadorA = crearUsuario(Rol.APROBADOR);
        Usuario aprobadorB = crearUsuario(Rol.APROBADOR);
        Espacio espacio = crearEspacio();
        Reserva pendiente = reservasRepo.saveAndFlush(new Reserva(solicitante, espacio,
                futuro(20, 10, 0).atZone(ZONA).toInstant(), futuro(20, 12, 0).atZone(ZONA).toInstant(), Instant.now()));

        CyclicBarrier barrera = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> aprobacion = pool.submit(decidir(actor(aprobadorA), pendiente.getId(), true, barrera));
            Future<String> rechazo = pool.submit(decidir(actor(aprobadorB), pendiente.getId(), false, barrera));

            String resultadoA = aprobacion.get();
            String resultadoB = rechazo.get();

            int exitos = cuenta(resultadoA, "OK") + cuenta(resultadoB, "OK");
            int conflictos = cuenta(resultadoA, "409") + cuenta(resultadoB, "409");
            assertEquals(1, exitos);
            assertEquals(1, conflictos);

            EstadoReserva estadoFinal = reservasRepo.findById(pendiente.getId()).orElseThrow().getEstado();
            EstadoReserva esperado = "OK".equals(resultadoA) ? EstadoReserva.APROBADA : EstadoReserva.RECHAZADA;
            assertEquals(esperado, estadoFinal);
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<String> decidir(UsuarioAutenticado actor, Long reservaId, boolean aprobar, CyclicBarrier barrera) {
        return () -> {
            barrera.await();
            try {
                if (aprobar) {
                    reservas.aprobar(actor, reservaId);
                } else {
                    reservas.rechazar(actor, reservaId, "Rechazo concurrente");
                }
                return "OK";
            } catch (DominioException ex) {
                return ex.getEstado() == HttpStatus.CONFLICT ? "409" : ex.getEstado().name();
            }
        };
    }

    private static int cuenta(String valor, String esperado) {
        return esperado.equals(valor) ? 1 : 0;
    }
}
