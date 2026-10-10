package es.uni.reservas.controller;

import es.uni.reservas.dto.EspacioDto;
import es.uni.reservas.service.EspacioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioController {

    private final EspacioService espacioService;

    public EspacioController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    @GetMapping
    public List<EspacioDto.Respuesta> listar() {
        return espacioService.listarActivos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EspacioDto.Respuesta crear(@Valid @RequestBody EspacioDto.Solicitud solicitud) {
        return espacioService.crear(solicitud);
    }

    @PutMapping("/{id}")
    public EspacioDto.Respuesta modificar(@PathVariable Long id, @Valid @RequestBody EspacioDto.Solicitud solicitud) {
        return espacioService.modificar(id, solicitud);
    }

    @PatchMapping("/{id}/desactivar")
    public EspacioDto.Respuesta desactivar(@PathVariable Long id) {
        return espacioService.desactivar(id);
    }
}
