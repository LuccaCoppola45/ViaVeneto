package ar.viaveneto.stock;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecetaItemRepository extends JpaRepository<RecetaItem, Long> {

	@Query("select r from RecetaItem r join fetch r.insumo join fetch r.producto")
	List<RecetaItem> findTodas();

	@Query("select r from RecetaItem r join fetch r.insumo where r.producto.id = :productoId")
	List<RecetaItem> findDeProducto(Long productoId);

	@Modifying
	@Query("delete from RecetaItem r where r.producto.id = :productoId")
	void borrarDeProducto(Long productoId);

	@Modifying
	@Query("delete from RecetaItem r where r.insumo.id = :insumoId")
	void borrarDeInsumo(Long insumoId);
}
