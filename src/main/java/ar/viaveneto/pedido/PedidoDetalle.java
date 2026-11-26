package ar.viaveneto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDetalle(
		Long id,
		TipoPedido tipo,
		EstadoPedido estado,
		/** "Mesa 4", "Para llevar · Juan", "Rappi · 1234". */
		String titulo,
		String mesaEtiqueta,
		String cliente,
		EstadoEntrega entrega,
		Long atiendeId,
		String atiendeNombre,
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
		BigDecimal saldo,
		/** Ya se hizo la factura en la registradora. */
		boolean facturado) {

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
		Long atiendeId = pedido.getAtiende() == null ? null : pedido.getAtiende().getId();
		String atiendeNombre = pedido.getAtiende() == null ? null : pedido.getAtiende().getNombre();
		return new PedidoDetalle(pedido.getId(), pedido.getTipo(), pedido.getEstado(), pedido.titulo(), mesaEtiqueta,
				pedido.getCliente(), pedido.getEntrega(), atiendeId, atiendeNombre,
				pedido.getAbiertoEn(), pedido.getCerradoEn(), items, pedido.subtotal(), pedido.getDescuentoPorcentaje(),
				pedido.descuento(), pedido.total(), pagos, pedido.pagado(), pedido.saldo(),
				pedido.isFacturado());
	}
}
