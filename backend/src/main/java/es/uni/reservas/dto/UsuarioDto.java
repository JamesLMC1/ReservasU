package es.uni.reservas.dto;

import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UsuarioDto {

    private UsuarioDto() {
    }

    public record CrearSolicitud(
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Email @Size(max = 190) String correo,
            @NotBlank @Size(min = 8, max = 100) String contrasena,
            @NotNull Rol rol) {
    }

    public record CambiarRolSolicitud(@NotNull Rol rol) {
    }

    public record Respuesta(Long id, String nombre, String correo, Rol rol, boolean activo) {

        public static Respuesta de(Usuario usuario) {
            return new Respuesta(usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                    usuario.getRol(), usuario.isActivo());
        }
    }
}
