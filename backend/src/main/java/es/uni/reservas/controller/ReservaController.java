package es.uni.reservas.controller;

import es.uni.reservas.dto.ReservaDto;
import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.security.UsuarioAutenticado;
import es.uni.reservas.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDto.Respuesta crear(@AuthenticationPrincipal UsuarioAutenticado actor,
                                      @Valid @RequestBody ReservaDto.CrearSolicitud solicitud) {
        return reservaService.crear(actor, solicitud);
    }

    @GetMapping
    public List<ReservaDto.Respuesta> listar(@AuthenticationPrincipal UsuarioAutenticado actor,
                                             @RequestParam(required = false) EstadoReserva estado) {
        return reservaService.listar(actor, Optional.ofNullable(estado));
    }

    @GetMapping("/{id}")
    public ReservaDto.Respuesta obtener(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable Long id) {
        return reservaService.obtener(actor, id);
    }

    @PostMapping("/{id}/cancelar")
    public ReservaDto.Respuesta cancelar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable Long id) {
        return reservaService.cancelar(actor, id);
    }

    @PostMapping("/{id}/aprobar")
    public ReservaDto.Respuesta aprobar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable Long id) {
        return reservaService.aprobar(actor, id);
    }

    @PostMapping("/{id}/rechazar")
    public ReservaDto.Respuesta rechazar(@AuthenticationPrincipal UsuarioAutenticado actor,
                                         @PathVariable Long id,
                                         @Valid @RequestBody ReservaDto.RechazoSolicitud solicitud) {
        return reservaService.rechazar(actor, id, solicitud.motivo());
    }

    @GetMapping("/{id}/historial")
    public List<ReservaDto.HistorialRespuesta> historial(@AuthenticationPrincipal UsuarioAutenticado actor,
                                                         @PathVariable Long id) {
        return reservaService.historial(actor, id);
    }
}
