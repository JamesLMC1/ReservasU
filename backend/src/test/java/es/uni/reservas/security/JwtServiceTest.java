package es.uni.reservas.security;

import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static es.uni.reservas.DatosPrueba.reloj;
import static es.uni.reservas.DatosPrueba.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-bytes!!";

    @Test
    @DisplayName("Un token generado devuelve el identificador del usuario")
    void tokenValidoDevuelveUsuario() {
        JwtService jwt = new JwtService(SECRETO, 30, Clock.systemUTC());
        Usuario usuario = usuario(7L, Rol.SOLICITANTE);

        String token = jwt.generar(usuario);

        assertEquals(7L, jwt.leerUsuarioId(token).orElseThrow());
    }

    @Test
    @DisplayName("Un token alterado no es aceptado")
    void tokenAlteradoNoEsAceptado() {
        JwtService jwt = new JwtService(SECRETO, 30, Clock.systemUTC());
        String token = jwt.generar(usuario(7L, Rol.SOLICITANTE));

        assertFalse(jwt.leerUsuarioId(token + "x").isPresent());
    }

    @Test
    @DisplayName("Un token caducado no es aceptado")
    void tokenCaducadoNoEsAceptado() {
        Clock pasado = Clock.fixed(Instant.parse("2020-01-01T08:00:00Z"), ZoneId.of("Europe/Madrid"));
        String token = new JwtService(SECRETO, 30, pasado).generar(usuario(7L, Rol.SOLICITANTE));

        assertFalse(new JwtService(SECRETO, 30, reloj()).leerUsuarioId(token).isPresent());
    }

    @Test
    @DisplayName("El secreto debe tener al menos 32 bytes")
    void secretoCortoEsRechazado() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", 30, reloj()));
    }
}
