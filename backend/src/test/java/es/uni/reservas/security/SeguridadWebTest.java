package es.uni.reservas.security;

import es.uni.reservas.config.SecurityConfig;
import es.uni.reservas.controller.AuthController;
import es.uni.reservas.controller.EspacioController;
import es.uni.reservas.controller.ReservaController;
import es.uni.reservas.controller.UsuarioController;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.service.AuthService;
import es.uni.reservas.service.EspacioService;
import es.uni.reservas.service.ReservaService;
import es.uni.reservas.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UsuarioController.class, EspacioController.class,
        ReservaController.class})
@Import(SecurityConfig.class)
class SeguridadWebTest {

    private static final String CUERPO_USUARIO = """
            {"nombre":"Ana","correo":"ana@uni.es","contrasena":"clave-segura-1","rol":"SOLICITANTE"}
            """;
    private static final String CUERPO_ESPACIO = """
            {"nombre":"Lab 1","tipo":"LABORATORIO"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private EspacioService espacioService;
    @MockitoBean
    private ReservaService reservaService;

    @Test
    @DisplayName("Sin token, las operaciones protegidas devuelven 401")
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/reservas")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El login es público")
    void loginEsPublico() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-es-correo\",\"contrasena\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    @DisplayName("Un solicitante no puede crear usuarios")
    void solicitanteNoCreaUsuarios() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_USUARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Un administrador puede crear usuarios")
    void administradorCreaUsuarios() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_USUARIO))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    @DisplayName("Un solicitante no puede gestionar espacios")
    void solicitanteNoGestionaEspacios() throws Exception {
        mvc.perform(post("/api/espacios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_ESPACIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    @DisplayName("Un solicitante no puede aprobar solicitudes")
    void solicitanteNoAprueba() throws Exception {
        mvc.perform(post("/api/reservas/1/aprobar")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "APROBADOR")
    @DisplayName("Un aprobador puede aprobar solicitudes")
    void aprobadorAprueba() throws Exception {
        mvc.perform(post("/api/reservas/1/aprobar")).andExpect(status().isOk());
    }
}
