package es.uni.reservas.repository;

import es.uni.reservas.entity.Rol;
import es.uni.reservas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByRol(Rol rol);

    List<Usuario> findAllByOrderByNombreAsc();
}
