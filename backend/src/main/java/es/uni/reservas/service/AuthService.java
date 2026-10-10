package es.uni.reservas.service;

import es.uni.reservas.dto.AuthDto;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthDto.LoginRespuesta login(AuthDto.LoginSolicitud solicitud) {
        Usuario usuario = usuarios.findByCorreo(solicitud.correo())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(solicitud.contrasena(), u.getContrasenaHash()))
                .orElseThrow(() -> new DominioException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        return new AuthDto.LoginRespuesta(jwtService.generar(usuario), usuario.getId(), usuario.getNombre(),
                usuario.getCorreo(), usuario.getRol());
    }
}
