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
		BigDecimal total) {

	public record Item(Long id, Long productoId, String nombre, BigDecimal precioUnitario, int cantidad, BigDecimal subtotal) {
	}

	static PedidoDetalle de(Pedido pedido) {
		List<Item> items = pedido.getItems().stream()
				.map(i -> new Item(i.getId(), i.getProducto().getId(), i.getNombre(), i.getPrecioUnitario(),
						i.getCantidad(), i.subtotal()))
				.toList();
		String mesaEtiqueta = pedido.getMesa() == null ? null : pedido.getMesa().etiqueta();
		return new PedidoDetalle(pedido.getId(), pedido.getTipo(), pedido.getEstado(), mesaEtiqueta,
				pedido.getAbiertoEn(), pedido.getCerradoEn(), items, pedido.total());
	}
}
