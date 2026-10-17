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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    private static final String CUERPO_RECHAZO = """
            {"motivo":"No disponible"}
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
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/espacios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El login es publico")
    void loginEsPublico() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-es-correo\",\"contrasena\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("No existe un endpoint publico de registro; devuelve 401 sin token")
    void noExisteRegistroPublico() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(CUERPO_USUARIO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    @DisplayName("Un solicitante no puede crear ni listar usuarios")
    void solicitanteNoGestionaUsuarios() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_USUARIO))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    @DisplayName("Un solicitante no puede gestionar espacios pero si listarlos")
    void solicitanteEspacios() throws Exception {
        mvc.perform(post("/api/espacios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_ESPACIO))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/espacios")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    @DisplayName("Un solicitante no puede aprobar ni rechazar")
    void solicitanteNoDecide() throws Exception {
        mvc.perform(post("/api/reservas/1/aprobar")).andExpect(status().isForbidden());
        mvc.perform(post("/api/reservas/1/rechazar").contentType(MediaType.APPLICATION_JSON).content(CUERPO_RECHAZO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "APROBADOR")
    @DisplayName("Un aprobador puede aprobar y rechazar, pero no gestionar espacios ni usuarios")
    void aprobadorDecide() throws Exception {
        mvc.perform(post("/api/reservas/1/aprobar")).andExpect(status().isOk());
        mvc.perform(post("/api/reservas/1/rechazar").contentType(MediaType.APPLICATION_JSON).content(CUERPO_RECHAZO))
                .andExpect(status().isOk());
        mvc.perform(post("/api/espacios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_ESPACIO))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Un rol o un tipo invalido devuelve 400")
    void valoresDeEnumeradoInvalidos400() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana@uni.es\",\"contrasena\":\"clave-segura-1\",\"rol\":\"INVALIDO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/espacios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Lab 1\",\"tipo\":\"INVALIDO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Un administrador puede gestionar usuarios, espacios y decisiones")
    void administradorGestionaTodo() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_USUARIO))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/usuarios")).andExpect(status().isOk());
        mvc.perform(post("/api/espacios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_ESPACIO))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/reservas/1/aprobar")).andExpect(status().isOk());
    }
}
