package ar.viaveneto.pedido;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class CantidadForm {

	private CantidadForm() {
	}

	public record AgregarItem(
			@NotNull(message = "Elegí un producto.") Long productoId,
			@Min(value = 1, message = "La cantidad tiene que ser 1 o más.")
			@Max(value = Pedido.MAXIMO_POR_ITEM, message = "Máximo 99 unidades por producto.") int cantidad) {
	}

	public record CambiarCantidad(
			@Min(value = 0, message = "La cantidad no puede ser negativa.")
			@Max(value = Pedido.MAXIMO_POR_ITEM, message = "Máximo 99 unidades por producto.") int cantidad) {
	}
}
