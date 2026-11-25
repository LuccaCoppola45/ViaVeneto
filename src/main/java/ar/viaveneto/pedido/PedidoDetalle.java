package ar.viaveneto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDetalle(
		Long id,
		TipoPedido tipo,
		EstadoPedido estado,
		String mesaEtiqueta,
		LocalDateTime abiertoEn,
		LocalDateTime cerradoEn,
		List<Item> items,
		BigDecimal subtotal,
		int descuentoPorcentaje,
		BigDecimal descuento,
		/** Lo que hay que cobrar, ya con el descuento. */
		BigDecimal total,
		List<PagoVista> pagos,
		BigDecimal pagado,
		/** Lo que falta cobrar. */
		BigDecimal saldo) {

	public record Item(Long id, Long productoId, String nombre, String nota, BigDecimal precioUnitario, int cantidad,
			BigDecimal subtotal) {
	}

	public record PagoVista(Long id, MedioPago medio, BigDecimal monto, BigDecimal propina, BigDecimal recibido,
			BigDecimal vuelto, LocalDateTime fecha) {
	}

	static PedidoDetalle de(Pedido pedido) {
		List<Item> items = pedido.getItems().stream()
				.map(i -> new Item(i.getId(), i.getProducto().getId(), i.getNombre(), i.getNota(), i.getPrecioUnitario(),
						i.getCantidad(), i.subtotal()))
				.toList();
		List<PagoVista> pagos = pedido.getPagos().stream()
				.map(p -> new PagoVista(p.getId(), p.getMedio(), p.getMonto(), p.getPropina(), p.getRecibido(), p.vuelto(),
						p.getFecha()))
				.toList();
		String mesaEtiqueta = pedido.getMesa() == null ? null : pedido.getMesa().etiqueta();
		return new PedidoDetalle(pedido.getId(), pedido.getTipo(), pedido.getEstado(), mesaEtiqueta,
				pedido.getAbiertoEn(), pedido.getCerradoEn(), items, pedido.subtotal(), pedido.getDescuentoPorcentaje(),
				pedido.descuento(), pedido.total(), pagos, pedido.pagado(), pedido.saldo());
	}
}
