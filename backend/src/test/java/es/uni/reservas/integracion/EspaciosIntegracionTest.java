package es.uni.reservas.integracion;

import es.uni.reservas.dto.EspacioDto;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.TipoEspacio;
import es.uni.reservas.exception.DominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class EspaciosIntegracionTest extends SoporteIntegracion {

    @Test
    @DisplayName("El servicio no permite dos espacios con el mismo nombre")
    void nombreDuplicadoEnServicio() {
        String nombre = "Espacio-" + UUID.randomUUID();
        espacioService.crear(new EspacioDto.Solicitud(nombre, TipoEspacio.LABORATORIO));

        DominioException ex = assertThrows(DominioException.class,
                () -> espacioService.crear(new EspacioDto.Solicitud(nombre, TipoEspacio.AUDITORIO)));

        assertEquals(HttpStatus.CONFLICT, ex.getEstado());
    }

    @Test
    @DisplayName("La base de datos rechaza el nombre duplicado aunque se inserte directamente")
    void nombreDuplicadoEnBaseDeDatos() {
        String nombre = "Espacio-" + UUID.randomUUID();
        espaciosRepo.saveAndFlush(new Espacio(nombre, TipoEspacio.LABORATORIO));

        assertThrows(DataIntegrityViolationException.class,
                () -> espaciosRepo.saveAndFlush(new Espacio(nombre, TipoEspacio.SALA_COMPUTO)));
    }
}
