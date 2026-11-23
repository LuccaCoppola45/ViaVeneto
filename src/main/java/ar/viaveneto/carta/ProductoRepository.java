package ar.viaveneto.carta;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

	@Query("select p from Producto p join fetch p.categoria c order by c.orden, c.nombre, p.nombre")
	List<Producto> findAllConCategoria();
}
