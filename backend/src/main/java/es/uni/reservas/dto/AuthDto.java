package es.uni.reservas.dto;

import es.uni.reservas.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDto {

    private AuthDto() {
    }

    public record LoginSolicitud(@NotBlank @Email String correo, @NotBlank String contrasena) {
    }

    public record LoginRespuesta(String token, Long id, String nombre, String correo, Rol rol) {
    }
}
