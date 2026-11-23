package ar.viaveneto.mesa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lo que muestra cada botón del plano de mesas. Si la mesa está libre, los datos del pedido vienen en null. */
public record MesaResumen(
		Long id,
		int numero,
		boolean ocupada,
		Long pedidoId,
		LocalDateTime abiertaDesde,
		int cantidadItems,
		BigDecimal total) {
}
