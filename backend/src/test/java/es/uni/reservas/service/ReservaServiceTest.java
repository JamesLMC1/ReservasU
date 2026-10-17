package es.uni.reservas.service;

import es.uni.reservas.dto.ReservaDto;
import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.Reserva;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.EspacioRepository;
import es.uni.reservas.repository.HistorialReservaRepository;
import es.uni.reservas.repository.ReservaRepository;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static es.uni.reservas.DatosPrueba.espacio;
import static es.uni.reservas.DatosPrueba.fecha;
import static es.uni.reservas.DatosPrueba.reloj;
import static es.uni.reservas.DatosPrueba.reserva;
import static es.uni.reservas.DatosPrueba.solicitud;
import static es.uni.reservas.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservas;
    @Mock
    private EspacioRepository espacios;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private HistorialReservaRepository historial;

    private final Clock clock = reloj();
    private ReservaService servicio;

    private final UsuarioAutenticado solicitante = new UsuarioAutenticado(2L, "usuario2@uni.es", Rol.SOLICITANTE);
    private final UsuarioAutenticado aprobador = new UsuarioAutenticado(3L, "usuario3@uni.es", Rol.APROBADOR);

    @BeforeEach
    void preparar() {
        servicio = new ReservaService(reservas, espacios, usuarios, historial, clock);
    }

    @Test
    @DisplayName("Crea una solicitud válida en estado PENDIENTE")
    void creaSolicitudValida() {
        Usuario dueno = usuario(2L, Rol.SOLICITANTE);
        when(espacios.findByIdParaActualizar(1L)).thenReturn(Optional.of(espacio(1L)));
        when(usuarios.findById(2L)).thenReturn(Optional.of(dueno));
        when(reservas.saveAndFlush(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        ReservaDto.Respuesta respuesta = servicio.crear(solicitante, solicitud(fecha(10, 0), fecha(14, 0)));

        assertEquals(EstadoReserva.PENDIENTE, respuesta.estado());
    }

    @Test
    @DisplayName("Acepta una reserva de exactamente cuatro horas")
    void aceptaCuatroHorasExactas() {
        when(espacios.findByIdParaActualizar(1L)).thenReturn(Optional.of(espacio(1L)));
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario(2L, Rol.SOLICITANTE)));
        when(reservas.saveAndFlush(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        ReservaDto.Respuesta respuesta = servicio.crear(solicitante, solicitud(fecha(10, 0), fecha(14, 0)));

        assertEquals(EstadoReserva.PENDIENTE, respuesta.estado());
    }

    @Test
    @DisplayName("Rechaza una reserva de más de cuatro horas")
    void rechazaMasDeCuatroHoras() {
        Executable accion = () -> servicio.crear(solicitante, solicitud(fecha(10, 0), fecha(14, 30)));

        assertEstado(HttpStatus.BAD_REQUEST, accion);
        verify(reservas, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Rechaza un fin que no es posterior al inicio")
    void rechazaFinAnteriorAlInicio() {
        assertEstado(HttpStatus.BAD_REQUEST,
                () -> servicio.crear(solicitante, solicitud(fecha(10, 0), fecha(10, 0))));
    }

    @Test
    @DisplayName("Rechaza horas que no son múltiplos de 30 minutos")
    void rechazaBloqueNoAlineado() {
        assertEstado(HttpStatus.BAD_REQUEST,
                () -> servicio.crear(solicitante, solicitud(fecha(10, 15), fecha(12, 0))));
    }

    @Test
    @DisplayName("Rechaza reservas con inicio en el pasado")
    void rechazaFechaPasada() {
        assertEstado(HttpStatus.BAD_REQUEST,
                () -> servicio.crear(solicitante, solicitud(fecha(9, 0).minusDays(3), fecha(10, 0).minusDays(3))));
    }

    @Test
    @DisplayName("Rechaza reservar un espacio desactivado")
    void rechazaEspacioDesactivado() {
        Espacio espacio = espacio(1L);
        espacio.desactivar();
        when(espacios.findByIdParaActualizar(1L)).thenReturn(Optional.of(espacio));

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.crear(solicitante, solicitud(fecha(10, 0), fecha(12, 0))));
    }

    @Test
    @DisplayName("Rechaza un solapamiento con una reserva activa del mismo espacio")
    void rechazaSolapamiento() {
        when(espacios.findByIdParaActualizar(1L)).thenReturn(Optional.of(espacio(1L)));
        when(reservas.contarSolapes(any(), anyCollection(), any(), any())).thenReturn(1L);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.crear(solicitante, solicitud(fecha(12, 0), fecha(13, 0))));
        verify(reservas, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Un aprobador aprueba una solicitud pendiente ajena")
    void apruebaSolicitudAjena() {
        Usuario dueno = usuario(2L, Rol.SOLICITANTE);
        Espacio espacio = espacio(1L);
        Reserva pendiente = reserva(10L, dueno, espacio, EstadoReserva.PENDIENTE);
        cargarParaDecision(pendiente, espacio);
        when(usuarios.findById(3L)).thenReturn(Optional.of(usuario(3L, Rol.APROBADOR)));
        when(reservas.contarSolapesExcepto(any(), anyCollection(), any(), any(), any())).thenReturn(0L);

        ReservaDto.Respuesta respuesta = servicio.aprobar(aprobador, 10L);

        assertEquals(EstadoReserva.APROBADA, respuesta.estado());
    }

    @Test
    @DisplayName("Nadie puede aprobar su propia solicitud")
    void rechazaAprobarPropia() {
        Usuario aprobadorDueno = usuario(3L, Rol.APROBADOR);
        Espacio espacio = espacio(1L);
        cargarParaDecision(reserva(10L, aprobadorDueno, espacio, EstadoReserva.PENDIENTE), espacio);

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.aprobar(aprobador, 10L));
    }

    @Test
    @DisplayName("Un solicitante no puede aprobar solicitudes")
    void solicitanteNoApruebe() {
        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.aprobar(solicitante, 10L));
    }

    @Test
    @DisplayName("Aprobar una solicitud que ya no está pendiente devuelve conflicto")
    void aprobarNoPendienteEsConflicto() {
        Espacio espacio = espacio(1L);
        cargarParaDecision(reserva(10L, usuario(2L, Rol.SOLICITANTE), espacio, EstadoReserva.RECHAZADA), espacio);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.aprobar(aprobador, 10L));
    }

    @Test
    @DisplayName("El rechazo exige un motivo")
    void rechazoExigeMotivo() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.rechazar(aprobador, 10L, "   "));
    }

    @Test
    @DisplayName("Rechaza una solicitud pendiente con motivo")
    void rechazaConMotivo() {
        Espacio espacio = espacio(1L);
        cargarParaDecision(reserva(10L, usuario(2L, Rol.SOLICITANTE), espacio, EstadoReserva.PENDIENTE), espacio);
        when(usuarios.findById(3L)).thenReturn(Optional.of(usuario(3L, Rol.APROBADOR)));

        ReservaDto.Respuesta respuesta = servicio.rechazar(aprobador, 10L, "Espacio en mantenimiento");

        assertEquals(EstadoReserva.RECHAZADA, respuesta.estado());
        assertEquals("Espacio en mantenimiento", respuesta.motivoRechazo());
    }

    @Test
    @DisplayName("Rechazar una solicitud que ya no está pendiente devuelve conflicto")
    void rechazarNoPendienteEsConflicto() {
        Espacio espacio = espacio(1L);
        cargarParaDecision(reserva(10L, usuario(2L, Rol.SOLICITANTE), espacio, EstadoReserva.APROBADA), espacio);

        assertEstado(HttpStatus.CONFLICT, () -> servicio.rechazar(aprobador, 10L, "Motivo"));
    }

    @Test
    @DisplayName("Un solicitante no puede cancelar la reserva de otro usuario")
    void solicitanteNoCancelaAjena() {
        Espacio espacio = espacio(1L);
        cargarParaDecision(reserva(10L, usuario(5L, Rol.SOLICITANTE), espacio, EstadoReserva.APROBADA), espacio);

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.cancelar(solicitante, 10L));
    }

    @Test
    @DisplayName("Un solicitante no puede ver la reserva de otro usuario")
    void solicitanteNoVeReservaAjena() {
        when(reservas.findById(10L)).thenReturn(Optional.of(reserva(10L, usuario(5L, Rol.SOLICITANTE), espacio(1L), EstadoReserva.PENDIENTE)));

        assertEstado(HttpStatus.FORBIDDEN, () -> servicio.obtener(solicitante, 10L));
    }

    @Test
    @DisplayName("El propietario cancela su reserva activa")
    void propietarioCancela() {
        Usuario dueno = usuario(2L, Rol.SOLICITANTE);
        Espacio espacio = espacio(1L);
        cargarParaDecision(reserva(10L, dueno, espacio, EstadoReserva.APROBADA), espacio);
        when(usuarios.findById(2L)).thenReturn(Optional.of(dueno));

        ReservaDto.Respuesta respuesta = servicio.cancelar(solicitante, 10L);

        assertEquals(EstadoReserva.CANCELADA, respuesta.estado());
    }

    @Test
    @DisplayName("No se puede cancelar una reserva ya cerrada")
    void noSePuedeCancelarReservaCerrada() {
        Usuario dueno = usuario(2L, Rol.SOLICITANTE);
        Espacio espacio = espacio(1L);
        cargarParaDecision(reserva(10L, dueno, espacio, EstadoReserva.RECHAZADA), espacio);

        assertEstado(HttpStatus.BAD_REQUEST, () -> servicio.cancelar(solicitante, 10L));
    }

    @Test
    @DisplayName("Un solicitante solo lista sus propias reservas")
    void solicitanteListaSoloPropias() {
        Usuario dueno = usuario(2L, Rol.SOLICITANTE);
        when(reservas.findByUsuarioIdOrderByInicioDesc(2L))
                .thenReturn(List.of(reserva(10L, dueno, espacio(1L), EstadoReserva.PENDIENTE)));

        List<ReservaDto.Respuesta> listado = servicio.listar(solicitante, Optional.empty());

        assertEquals(1, listado.size());
        verify(reservas, never()).findAllByOrderByInicioDesc();
    }

    /** Prepara las búsquedas que hace bloquearReserva: primero el espacio, después la reserva. */
    private void cargarParaDecision(Reserva reserva, Espacio espacio) {
        when(reservas.findEspacioIdPorReserva(reserva.getId())).thenReturn(Optional.of(espacio.getId()));
        when(espacios.findByIdParaActualizar(espacio.getId())).thenReturn(Optional.of(espacio));
        when(reservas.findByIdParaActualizar(reserva.getId())).thenReturn(Optional.of(reserva));
    }

    private static void assertEstado(HttpStatus esperado, Executable accion) {
        DominioException ex = assertThrows(DominioException.class, accion);
        assertEquals(esperado, ex.getEstado());
    }
}
