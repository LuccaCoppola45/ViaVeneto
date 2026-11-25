package ar.viaveneto.pedido;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	@Query("""
			select distinct p from Pedido p
			join fetch p.mesa
			left join fetch p.atiende
			left join fetch p.items
			where p.estado = ar.viaveneto.pedido.EstadoPedido.ABIERTO and p.tipo = ar.viaveneto.pedido.TipoPedido.MESA
			""")
	List<Pedido> findAbiertosDeMesasConItems();

	@Query("""
			select p from Pedido p
			where p.mesa.id = :mesaId and p.estado = ar.viaveneto.pedido.EstadoPedido.ABIERTO
			""")
	Optional<Pedido> findAbiertoDeMesa(Long mesaId);

	/** Los de llevar y Rappi que siguen en curso: sin cobrar, o cobrados pero todavía no entregados. */
	@Query("""
			select distinct p from Pedido p
			left join fetch p.atiende
			left join fetch p.items
			where p.tipo <> ar.viaveneto.pedido.TipoPedido.MESA
			and (p.estado = ar.viaveneto.pedido.EstadoPedido.ABIERTO
				or (p.estado = ar.viaveneto.pedido.EstadoPedido.CERRADO and p.entrega <> ar.viaveneto.pedido.EstadoEntrega.ENTREGADO))
			order by p.abiertoEn
			""")
	List<Pedido> findSinMesaEnCurso();

	@Query("select p from Pedido p left join fetch p.mesa left join fetch p.atiende left join fetch p.items where p.id = :id")
	Optional<Pedido> findDetalle(Long id);

	/** Bloquea el pedido mientras se modifica, por si dos personas cargan la misma mesa a la vez. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pedido p where p.id = :id")
	Optional<Pedido> findByIdParaActualizar(Long id);
}
