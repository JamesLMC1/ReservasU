package es.uni.reservas.integracion;

import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.TipoEspacio;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.repository.EspacioRepository;
import es.uni.reservas.repository.HistorialReservaRepository;
import es.uni.reservas.repository.ReservaRepository;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.UsuarioAutenticado;
import es.uni.reservas.service.EspacioService;
import es.uni.reservas.service.ReservaService;
import es.uni.reservas.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base para las pruebas de integración contra PostgreSQL real (base reservas_test).
 * Requiere que Flyway haya aplicado las migraciones.
 */
@SpringBootTest
abstract class SoporteIntegracion {

    @Autowired
    protected ReservaService reservas;
    @Autowired
    protected UsuarioService usuarioService;
    @Autowired
    protected EspacioService espacioService;
    @Autowired
    protected ReservaRepository reservasRepo;
    @Autowired
    protected UsuarioRepository usuariosRepo;
    @Autowired
    protected EspacioRepository espaciosRepo;
    @Autowired
    protected HistorialReservaRepository historialRepo;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected Usuario crearUsuario(Rol rol) {
        return usuariosRepo.save(new Usuario("Prueba " + rol, correoUnico(), passwordEncoder.encode("clave-segura-1"), rol));
    }

    protected Espacio crearEspacio() {
        return espaciosRepo.save(new Espacio("Espacio-" + UUID.randomUUID(), TipoEspacio.LABORATORIO));
    }

    protected UsuarioAutenticado actor(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getCorreo(), usuario.getRol());
    }

    protected String correoUnico() {
        return "prueba-" + UUID.randomUUID() + "@uni.es";
    }

    /** Fecha futura para evitar el rechazo por reservas en el pasado. */
    protected static LocalDateTime futuro(int dia, int hora, int minuto) {
        return LocalDateTime.of(2030, 1, dia, hora, minuto);
    }
}
