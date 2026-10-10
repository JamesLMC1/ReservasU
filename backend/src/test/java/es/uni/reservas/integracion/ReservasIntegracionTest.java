package es.uni.reservas.integracion;

import es.uni.reservas.dto.ReservaDto;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.TipoEspacio;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.EspacioRepository;
import es.uni.reservas.repository.UsuarioRepository;
import es.uni.reservas.security.UsuarioAutenticado;
import es.uni.reservas.service.ReservaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas contra PostgreSQL real. Solo se ejecutan si la variable DB_PASSWORD está definida.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ReservasIntegracionTest {

    @Autowired
    private ReservaService reservas;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private EspacioRepository espacios;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Dos solicitudes simultáneas para el mismo espacio e intervalo: solo una queda registrada")
    void solicitudesSimultaneasSoloUnaQueda() throws Exception {
        Usuario usuario = usuarios.save(new Usuario("Concurrencia", "concurrencia-" + UUID.randomUUID() + "@uni.es",
                passwordEncoder.encode("clave-segura-1"), Rol.SOLICITANTE));
        Espacio espacio = espacios.save(new Espacio("Lab-" + UUID.randomUUID(), TipoEspacio.LABORATORIO));
        UsuarioAutenticado actor = new UsuarioAutenticado(usuario.getId(), usuario.getCorreo(), usuario.getRol());
        ReservaDto.CrearSolicitud solicitud = new ReservaDto.CrearSolicitud(
                espacio.getId(), LocalDateTime.of(2030, 1, 10, 10, 0), LocalDateTime.of(2030, 1, 10, 12, 0));

        int hilos = 2;
        CyclicBarrier barrera = new CyclicBarrier(hilos);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Future<String>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                resultados.add(pool.submit(intentar(actor, solicitud, barrera)));
            }
            int exitos = 0;
            int conflictos = 0;
            for (Future<String> resultado : resultados) {
                String estado = resultado.get();
                if ("OK".equals(estado)) {
                    exitos++;
                } else if ("409".equals(estado)) {
                    conflictos++;
                }
            }
            assertEquals(1, exitos);
            assertEquals(1, conflictos);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Una reserva de más de cuatro horas es rechazada")
    void duracionMaximaSeRespeta() {
        Usuario usuario = usuarios.save(new Usuario("Duración", "duracion-" + UUID.randomUUID() + "@uni.es",
                passwordEncoder.encode("clave-segura-1"), Rol.SOLICITANTE));
        Espacio espacio = espacios.save(new Espacio("Aud-" + UUID.randomUUID(), TipoEspacio.AUDITORIO));
        UsuarioAutenticado actor = new UsuarioAutenticado(usuario.getId(), usuario.getCorreo(), usuario.getRol());

        DominioException ex = org.junit.jupiter.api.Assertions.assertThrows(DominioException.class,
                () -> reservas.crear(actor, new ReservaDto.CrearSolicitud(espacio.getId(),
                        LocalDateTime.of(2030, 1, 10, 10, 0), LocalDateTime.of(2030, 1, 10, 14, 30))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getEstado());
    }

    private Callable<String> intentar(UsuarioAutenticado actor, ReservaDto.CrearSolicitud solicitud,
                                      CyclicBarrier barrera) {
        return () -> {
            barrera.await();
            try {
                reservas.crear(actor, solicitud);
                return "OK";
            } catch (DominioException ex) {
                return ex.getEstado() == HttpStatus.CONFLICT ? "409" : ex.getEstado().name();
            }
        };
    }
}
