package ar.viaveneto.pedido;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PagoRepository extends JpaRepository<Pago, Long> {

	@Query("select p from Pago p join fetch p.pedido where p.turno.id in :turnos")
	List<Pago> findByTurnos(Collection<Long> turnos);
}
