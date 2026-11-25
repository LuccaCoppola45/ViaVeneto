package ar.viaveneto.mesa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lo que muestra cada mesa en el plano: cómo se llama, dónde está, cuántas mesitas tiene
 * y, si está ocupada, cuánto lleva. Si la mesa está libre, los datos del pedido vienen en null.
 */
public record MesaResumen(
		Long id,
		int numero,
		String etiqueta,
		int mesitas,
		Long sectorId,
		int x,
		int y,
		boolean vertical,
		boolean ocupada,
		Long pedidoId,
		/** La moza que atiende la mesa ocupada. */
		Long atiendeId,
		String atiendeNombre,
		LocalDateTime abiertaDesde,
		int cantidadItems,
		BigDecimal total) {
}
