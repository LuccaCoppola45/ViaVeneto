package ar.viaveneto.stock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

	List<Insumo> findByActivoTrueOrderByNombre();

	Optional<Insumo> findByNombreIgnoreCase(String nombre);

	/** Bloquea los insumos mientras se descuentan, por si dos mozos cargan lo último a la vez. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Insumo i where i.id in :ids order by i.id")
	List<Insumo> findParaActualizar(Collection<Long> ids);
}
