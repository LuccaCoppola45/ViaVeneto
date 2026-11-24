package ar.viaveneto.mesa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lo que muestra cada mesa en el plano: dónde está y, si está ocupada, cuánto lleva.
 * Si la mesa está libre, los datos del pedido vienen en null.
 */
public record MesaResumen(
		Long id,
		int numero,
		Long sectorId,
		int x,
		int y,
		boolean vertical,
		boolean ocupada,
		Long pedidoId,
		LocalDateTime abiertaDesde,
		int cantidadItems,
		BigDecimal total) {
}
