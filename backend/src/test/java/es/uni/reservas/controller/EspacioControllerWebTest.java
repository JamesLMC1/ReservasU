package es.uni.reservas.controller;

import es.uni.reservas.config.SecurityConfig;
import es.uni.reservas.dto.EspacioDto;
import es.uni.reservas.entity.TipoEspacio;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.JwtService;
import es.uni.reservas.service.EspacioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EspacioController.class)
@Import(SecurityConfig.class)
class EspacioControllerWebTest {

    private static final String CUERPO = """
            {"nombre":"Lab 1","tipo":"LABORATORIO"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private EspacioService espacioService;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("El administrador crea un espacio y recibe 201")
    void administradorCreaEspacio() throws Exception {
        when(espacioService.crear(any())).thenReturn(new EspacioDto.Respuesta(1L, "Lab 1", TipoEspacio.LABORATORIO, true));

        mvc.perform(post("/api/espacios").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Lab 1"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("El administrador modifica y desactiva un espacio")
    void administradorModificaYDesactiva() throws Exception {
        when(espacioService.modificar(eq(1L), any()))
                .thenReturn(new EspacioDto.Respuesta(1L, "Auditorio", TipoEspacio.AUDITORIO, true));
        when(espacioService.desactivar(1L))
                .thenReturn(new EspacioDto.Respuesta(1L, "Auditorio", TipoEspacio.AUDITORIO, false));

        mvc.perform(put("/api/espacios/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Auditorio\",\"tipo\":\"AUDITORIO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("AUDITORIO"));
        mvc.perform(patch("/api/espacios/1/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    @DisplayName("Cualquier usuario autenticado lista los espacios activos")
    void listaEspaciosAutenticado() throws Exception {
        when(espacioService.listarActivos())
                .thenReturn(List.of(new EspacioDto.Respuesta(1L, "Lab 1", TipoEspacio.LABORATORIO, true)));

        mvc.perform(get("/api/espacios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Lab 1"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Modificar un espacio inexistente devuelve 404")
    void modificarInexistente404() throws Exception {
        when(espacioService.modificar(eq(99L), any()))
                .thenThrow(new DominioException(HttpStatus.NOT_FOUND, "Espacio no encontrado"));

        mvc.perform(put("/api/espacios/99").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Espacio no encontrado"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Un nombre duplicado devuelve 409")
    void nombreDuplicado409() throws Exception {
        when(espacioService.crear(any()))
                .thenThrow(new DominioException(HttpStatus.CONFLICT, "Ya existe un espacio con ese nombre"));

        mvc.perform(post("/api/espacios").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict());
    }
}
