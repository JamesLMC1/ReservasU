package es.uni.reservas.repository;

import es.uni.reservas.entity.HistorialReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialReservaRepository extends JpaRepository<HistorialReserva, Long> {

    List<HistorialReserva> findByReservaIdOrderByFechaAsc(Long reservaId);
}
