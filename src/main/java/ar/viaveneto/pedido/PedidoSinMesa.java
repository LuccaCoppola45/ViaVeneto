package ar.viaveneto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Tarjetita de la columna "Para llevar y Rappi" en la pantalla de mesas. */
public record PedidoSinMesa(
		Long id,
		TipoPedido tipo,
		String titulo,
		String cliente,
		EstadoPedido estado,
		EstadoEntrega entrega,
		LocalDateTime abiertoEn,
		int unidades,
		BigDecimal total,
		BigDecimal saldo,
		String atiendeNombre) {

	static PedidoSinMesa de(Pedido p) {
		return new PedidoSinMesa(p.getId(), p.getTipo(), p.titulo(), p.getCliente(), p.getEstado(), p.getEntrega(),
				p.getAbiertoEn(), p.cantidadDeUnidades(), p.total(), p.saldo(),
				p.getAtiende() == null ? null : p.getAtiende().getNombre());
	}
}
