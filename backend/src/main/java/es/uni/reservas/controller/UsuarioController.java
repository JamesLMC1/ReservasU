package es.uni.reservas.controller;

import es.uni.reservas.dto.UsuarioDto;
import es.uni.reservas.security.UsuarioAutenticado;
import es.uni.reservas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto.Respuesta crear(@Valid @RequestBody UsuarioDto.CrearSolicitud solicitud) {
        return usuarioService.crear(solicitud);
    }

    @GetMapping
    public List<UsuarioDto.Respuesta> listar() {
        return usuarioService.listar();
    }

    @PatchMapping("/{id}/rol")
    public UsuarioDto.Respuesta cambiarRol(@PathVariable Long id,
                                           @Valid @RequestBody UsuarioDto.CambiarRolSolicitud solicitud) {
        return usuarioService.cambiarRol(id, solicitud);
    }

    @PatchMapping("/{id}/desactivar")
    public UsuarioDto.Respuesta desactivar(@AuthenticationPrincipal UsuarioAutenticado actor,
                                           @PathVariable Long id) {
        return usuarioService.desactivar(actor, id);
    }
}
