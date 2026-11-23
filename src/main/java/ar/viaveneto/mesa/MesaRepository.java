package ar.viaveneto.mesa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

	List<Mesa> findByActivaTrueOrderByNumero();

	Optional<Mesa> findByNumero(int numero);

	/** Bloquea la fila para que dos tablets no abran la misma mesa a la vez. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Mesa m where m.id = :id")
	Optional<Mesa> findByIdParaActualizar(Long id);
}
