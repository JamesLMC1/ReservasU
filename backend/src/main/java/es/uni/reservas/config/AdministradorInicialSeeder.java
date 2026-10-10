package es.uni.reservas.config;

import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el primer administrador al arrancar, usando ADMIN_EMAIL y ADMIN_PASSWORD.
 * No existe ningún endpoint público de registro.
 */
@Component
public class AdministradorInicialSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String correo;
    private final String contrasena;

    public AdministradorInicialSeeder(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                                      @Value("${app.admin.correo}") String correo,
                                      @Value("${app.admin.contrasena}") String contrasena) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.correo = correo;
        this.contrasena = contrasena;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (correo == null || correo.isBlank() || contrasena == null || contrasena.length() < 8) {
            return;
        }
        if (usuarios.existsByRol(Rol.ADMINISTRADOR)) {
            return;
        }
        usuarios.save(new Usuario("Administrador", correo, passwordEncoder.encode(contrasena), Rol.ADMINISTRADOR));
    }
}
