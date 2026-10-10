package es.uni.reservas.service;

import es.uni.reservas.dto.EspacioDto;
import es.uni.reservas.entity.Espacio;
import es.uni.reservas.exception.DominioException;
import es.uni.reservas.repository.EspacioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EspacioService {

    private final EspacioRepository espacios;

    public EspacioService(EspacioRepository espacios) {
        this.espacios = espacios;
    }

    @Transactional
    public EspacioDto.Respuesta crear(EspacioDto.Solicitud solicitud) {
        if (espacios.existsByNombre(solicitud.nombre())) {
            throw new DominioException(HttpStatus.CONFLICT, "Ya existe un espacio con ese nombre");
        }
        return EspacioDto.Respuesta.de(espacios.save(new Espacio(solicitud.nombre(), solicitud.tipo())));
    }

    @Transactional
    public EspacioDto.Respuesta modificar(Long id, EspacioDto.Solicitud solicitud) {
        Espacio espacio = buscar(id);
        if (espacios.existsByNombreAndIdNot(solicitud.nombre(), id)) {
            throw new DominioException(HttpStatus.CONFLICT, "Ya existe un espacio con ese nombre");
        }
        espacio.modificar(solicitud.nombre(), solicitud.tipo());
        return EspacioDto.Respuesta.de(espacio);
    }

    @Transactional
    public EspacioDto.Respuesta desactivar(Long id) {
        Espacio espacio = buscar(id);
        espacio.desactivar();
        return EspacioDto.Respuesta.de(espacio);
    }

    @Transactional(readOnly = true)
    public List<EspacioDto.Respuesta> listarActivos() {
        return espacios.findByActivoTrueOrderByNombreAsc().stream().map(EspacioDto.Respuesta::de).toList();
    }

    private Espacio buscar(Long id) {
        return espacios.findById(id)
                .orElseThrow(() -> new DominioException(HttpStatus.NOT_FOUND, "Espacio no encontrado"));
    }
}
