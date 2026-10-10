package es.uni.reservas.repository;

import es.uni.reservas.entity.EstadoReserva;
import es.uni.reservas.entity.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("select count(r) from Reserva r where r.espacio.id = :espacioId and r.estado in :estados "
            + "and r.inicio < :fin and r.fin > :inicio")
    long contarSolapes(@Param("espacioId") Long espacioId,
                       @Param("estados") Collection<EstadoReserva> estados,
                       @Param("inicio") Instant inicio,
                       @Param("fin") Instant fin);

    @Query("select count(r) from Reserva r where r.espacio.id = :espacioId and r.estado in :estados "
            + "and r.inicio < :fin and r.fin > :inicio and r.id <> :excluirId")
    long contarSolapesExcepto(@Param("espacioId") Long espacioId,
                              @Param("estados") Collection<EstadoReserva> estados,
                              @Param("inicio") Instant inicio,
                              @Param("fin") Instant fin,
                              @Param("excluirId") Long excluirId);

    @Query("select r.espacio.id from Reserva r where r.id = :id")
    Optional<Long> findEspacioIdPorReserva(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdParaActualizar(@Param("id") Long id);

    List<Reserva> findByUsuarioIdOrderByInicioDesc(Long usuarioId);

    List<Reserva> findAllByOrderByInicioDesc();

    List<Reserva> findByEstadoOrderByInicioAsc(EstadoReserva estado);

    List<Reserva> findByUsuarioIdAndEstado(Long usuarioId, EstadoReserva estado);
}
