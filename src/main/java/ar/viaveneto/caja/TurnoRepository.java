package ar.viaveneto.caja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

	Optional<Turno> findFirstByCerradoEnIsNullOrderByAbiertoEnDesc();

	Optional<Turno> findFirstByCerradoEnIsNotNullOrderByCerradoEnDesc();

	List<Turno> findByAbiertoEnGreaterThanEqualOrderByAbiertoEn(LocalDateTime desde);
}
