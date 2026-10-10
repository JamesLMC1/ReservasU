package es.uni.reservas.service;

import es.uni.reservas.dto.AuthDto;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static es.uni.reservas.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @Test
    @DisplayName("Rechaza una contraseña incorrecta sin revelar si el correo existe")
    void rechazaContrasenaIncorrecta() {
        Usuario usuario = usuario(2L, Rol.SOLICITANTE);
        when(usuarios.findByCorreo(anyString())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), any())).thenReturn(false);
        AuthService servicio = new AuthService(usuarios, passwordEncoder, jwtService);

        DominioException ex = assertThrows(DominioException.class,
                () -> servicio.login(new AuthDto.LoginSolicitud("usuario2@uni.es", "incorrecta")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getEstado());
    }

    @Test
    @DisplayName("Rechaza el inicio de sesión de un usuario desactivado")
    void rechazaUsuarioDesactivado() {
        Usuario usuario = usuario(2L, Rol.SOLICITANTE);
        usuario.desactivar();
        when(usuarios.findByCorreo(anyString())).thenReturn(Optional.of(usuario));
        AuthService servicio = new AuthService(usuarios, passwordEncoder, jwtService);

        DominioException ex = assertThrows(DominioException.class,
                () -> servicio.login(new AuthDto.LoginSolicitud("usuario2@uni.es", "clave-segura-1")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getEstado());
    }

    @Test
    @DisplayName("Devuelve un token con credenciales correctas")
    void devuelveTokenConCredencialesCorrectas() {
        Usuario usuario = usuario(2L, Rol.APROBADOR);
        when(usuarios.findByCorreo(anyString())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(anyString(), any())).thenReturn(true);
        when(jwtService.generar(usuario)).thenReturn("token-de-prueba");
        AuthService servicio = new AuthService(usuarios, passwordEncoder, jwtService);

        AuthDto.LoginRespuesta respuesta = servicio.login(new AuthDto.LoginSolicitud("usuario2@uni.es", "clave-segura-1"));

        assertEquals("token-de-prueba", respuesta.token());
        assertEquals(Rol.APROBADOR, respuesta.rol());
    }
}
