package es.uni.reservas.integracion;

import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.HistorialReserva;
import es.uni.reservas.entity.Reserva;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.repository.HistorialReservaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class HistorialTransaccionalIntegracionTest extends SoporteIntegracion {

    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

    @MockitoSpyBean
    private HistorialReservaRepository historialEspiado;

    @Test
    @DisplayName("Un cambio de estado registra su fila de historial en la misma transaccion")
    void cambioDeEstadoRegistraHistorial() {
        Usuario dueno = crearUsuario(Rol.SOLICITANTE);
        Espacio espacio = crearEspacio();
        Reserva reserva = reservasRepo.saveAndFlush(new Reserva(dueno, espacio,
                futuro(21, 10, 0).atZone(ZONA).toInstant(), futuro(21, 12, 0).atZone(ZONA).toInstant(), Instant.now()));

        reservas.cancelar(actor(dueno), reserva.getId());

        List<HistorialReserva> historial = historialRepo.findByReservaIdOrderByFechaAsc(reserva.getId());
        assertEquals(1, historial.size());
        assertEquals(EstadoReserva.CANCELADA, historial.get(0).getEstadoNuevo());
    }

    @Test
    @DisplayName("Si falla el historial, el cambio de estado se revierte")
    void falloEnHistorialRevierteElCambio() {
        Usuario dueno = crearUsuario(Rol.SOLICITANTE);
        Espacio espacio = crearEspacio();
        Reserva reserva = reservasRepo.saveAndFlush(new Reserva(dueno, espacio,
                futuro(22, 10, 0).atZone(ZONA).toInstant(), futuro(22, 12, 0).atZone(ZONA).toInstant(), Instant.now()));

        doThrow(new IllegalStateException("fallo simulado del historial"))
                .when(historialEspiado).save(any(HistorialReserva.class));

        assertThrows(IllegalStateException.class, () -> reservas.cancelar(actor(dueno), reserva.getId()));

        EstadoReserva estadoFinal = reservasRepo.findById(reserva.getId()).orElseThrow().getEstado();
        assertEquals(EstadoReserva.PENDIENTE, estadoFinal);
        assertTrue(historialRepo.findByReservaIdOrderByFechaAsc(reserva.getId()).isEmpty());
    }
}
