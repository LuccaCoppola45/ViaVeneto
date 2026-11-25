package ar.viaveneto.acceso;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	List<Usuario> findByActivoTrueOrderByNombre();

	Optional<Usuario> findByNombreIgnoreCase(String nombre);

	boolean existsByActivoTrue();

	long countByActivoTrue();
}
