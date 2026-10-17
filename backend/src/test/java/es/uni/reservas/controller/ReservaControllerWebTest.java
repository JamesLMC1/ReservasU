package es.uni.reservas.controller;

import es.uni.reservas.config.SecurityConfig;
import es.uni.reservas.dto.ReservaDto;
import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.JwtService;
import es.uni.reservas.security.UsuarioAutenticado;
import es.uni.reservas.service.ReservaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservaController.class)
@Import(SecurityConfig.class)
class ReservaControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private ReservaService reservaService;

    @Test
    @DisplayName("Crear una reserva autenticado devuelve 201 y estado PENDIENTE")
    void crearReservaDevuelvePendiente() throws Exception {
        when(reservaService.crear(any(), any())).thenReturn(respuesta(1L, EstadoReserva.PENDIENTE));

        mvc.perform(post("/api/reservas").with(autenticado(2L, Rol.SOLICITANTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"espacioId\":1,\"inicio\":\"2030-01-10T10:00:00\",\"fin\":\"2030-01-10T12:00:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("Un solicitante lista reservas")
    void solicitanteListaReservas() throws Exception {
        when(reservaService.listar(any(), any())).thenReturn(List.of(respuesta(1L, EstadoReserva.PENDIENTE)));

        mvc.perform(get("/api/reservas").with(autenticado(2L, Rol.SOLICITANTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("Un aprobador lista reservas")
    void aprobadorListaReservas() throws Exception {
        when(reservaService.listar(any(), any())).thenReturn(List.of(respuesta(1L, EstadoReserva.APROBADA)));

        mvc.perform(get("/api/reservas").with(autenticado(3L, Rol.APROBADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("APROBADA"));
    }

    @Test
    @DisplayName("El historial de una reserva devuelve 200")
    void historialDevuelve200() throws Exception {
        when(reservaService.historial(any(), any())).thenReturn(List.of(new ReservaDto.HistorialRespuesta(
                null, EstadoReserva.PENDIENTE, 2L, "ana@uni.es", OffsetDateTime.now(), "Solicitud creada")));

        mvc.perform(get("/api/reservas/1/historial").with(autenticado(2L, Rol.SOLICITANTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoNuevo").value("PENDIENTE"));
    }

    @Test
    @DisplayName("Rechazar sin motivo devuelve 400")
    void rechazarSinMotivo400() throws Exception {
        mvc.perform(post("/api/reservas/1/rechazar").with(autenticado(3L, Rol.APROBADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private static ReservaDto.Respuesta respuesta(Long id, EstadoReserva estado) {
        return new ReservaDto.Respuesta(id, 1L, "Lab 1", 2L, "ana@uni.es",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(2), estado, null);
    }

    private static RequestPostProcessor autenticado(Long id, Rol rol) {
        UsuarioAutenticado principal = new UsuarioAutenticado(id, "usuario@uni.es", rol);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
    }
}
