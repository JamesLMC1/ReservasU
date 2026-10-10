package es.uni.reservas.repository;

import es.uni.reservas.entity.Espacio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);

    List<Espacio> findByActivoTrueOrderByNombreAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espacio e where e.id = :id")
    Optional<Espacio> findByIdParaActualizar(@Param("id") Long id);
}
