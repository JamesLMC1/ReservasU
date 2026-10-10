package es.uni.reservas.dto;

import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.TipoEspacio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class EspacioDto {

    private EspacioDto() {
    }

    public record Solicitud(@NotBlank @Size(max = 120) String nombre, @NotNull TipoEspacio tipo) {
    }

    public record Respuesta(Long id, String nombre, TipoEspacio tipo, boolean activo) {

        public static Respuesta de(Espacio espacio) {
            return new Respuesta(espacio.getId(), espacio.getNombre(), espacio.getTipo(), espacio.isActivo());
        }
    }
}
