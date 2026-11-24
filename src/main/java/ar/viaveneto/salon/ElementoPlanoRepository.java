package ar.viaveneto.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ElementoPlanoRepository extends JpaRepository<ElementoPlano, Long> {

	List<ElementoPlano> findAllByOrderById();
}
