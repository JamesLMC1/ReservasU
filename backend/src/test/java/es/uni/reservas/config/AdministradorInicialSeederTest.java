package es.uni.reservas.config;

import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import es.uni.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministradorInicialSeederTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Crea el administrador cuando no existe ninguno")
    void creaAdministradorCuandoNoExiste() {
        when(usuarios.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        when(passwordEncoder.encode("clave-segura-1")).thenReturn("hash");
        AdministradorInicialSeeder seeder = new AdministradorInicialSeeder(usuarios, passwordEncoder,
                "admin@uni.es", "clave-segura-1");

        seeder.run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        assertEquals(Rol.ADMINISTRADOR, captor.getValue().getRol());
        assertEquals("admin@uni.es", captor.getValue().getCorreo());
    }

    @Test
    @DisplayName("No crea nada si ya existe un administrador")
    void noCreaSiYaExiste() {
        when(usuarios.existsByRol(Rol.ADMINISTRADOR)).thenReturn(true);
        AdministradorInicialSeeder seeder = new AdministradorInicialSeeder(usuarios, passwordEncoder,
                "admin@uni.es", "clave-segura-1");

        seeder.run(null);

        verify(usuarios, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("No crea nada si falta la configuracion del administrador")
    void noCreaSinConfiguracion() {
        AdministradorInicialSeeder seeder = new AdministradorInicialSeeder(usuarios, passwordEncoder, "", "");

        seeder.run(null);

        verify(usuarios, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
