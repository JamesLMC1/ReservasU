package es.uni.reservas.service;

import es.uni.reservas.dto.EspacioDto;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.TipoEspacio;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.EspacioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static es.uni.reservas.DatosPrueba.espacio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspacioServiceTest {

    @Mock
    private EspacioRepository espacios;

    @InjectMocks
    private EspacioService servicio;

    @Test
    @DisplayName("No permite dos espacios con el mismo nombre")
    void rechazaNombreDuplicado() {
        when(espacios.existsByNombre("Lab 1")).thenReturn(true);
        EspacioDto.Solicitud solicitud = new EspacioDto.Solicitud("Lab 1", TipoEspacio.LABORATORIO);

        DominioException ex = assertThrows(DominioException.class, () -> servicio.crear(solicitud));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    @DisplayName("Modificar un espacio inexistente devuelve 404")
    void modificarInexistenteDevuelve404() {
        when(espacios.findById(99L)).thenReturn(Optional.empty());
        EspacioDto.Solicitud solicitud = new EspacioDto.Solicitud("Auditorio", TipoEspacio.AUDITORIO);

        DominioException ex = assertThrows(DominioException.class, () -> servicio.modificar(99L, solicitud));

        assertEquals(HttpStatus.NOT_FOUND, ex.getEstado());
    }

    @Test
    @DisplayName("Desactivar marca el espacio como inactivo")
    void desactivaEspacio() {
        Espacio espacio = espacio(1L);
        when(espacios.findById(1L)).thenReturn(Optional.of(espacio));

        EspacioDto.Respuesta respuesta = servicio.desactivar(1L);

        assertFalse(respuesta.activo());
    }
}
