package ar.viaveneto.pedido;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

	public record NuevoSinMesa(
			@NotNull(message = "Elegí si es para llevar o Rappi.") TipoPedido tipo,
			@Size(max = 60, message = "El nombre es muy largo (máximo 60 letras).") String cliente) {
	}

	public record Facturado(boolean facturado) {
	}

	public record Entrega(@NotNull(message = "Elegí el estado.") EstadoEntrega estado) {
	}

	public record Atiende(@NotNull(message = "Elegí quién atiende.") Long usuarioId) {
	}

	public record Nota(
			@Size(max = ItemPedido.LARGO_NOTA, message = "La nota es muy larga (máximo 80 letras).") String nota,
			boolean soloUna) {
	}

	public record Descuento(
			@Min(value = 0, message = "El descuento no puede ser negativo.")
			@Max(value = 100, message = "El descuento no puede pasar el 100%.") int porcentaje) {
	}

	public record Cobro(
			@NotNull(message = "Elegí cómo paga.") MedioPago medio,
			@NotNull(message = "Poné cuánto paga.") @DecimalMin(value = "1", message = "Poné cuánto paga.") BigDecimal monto,
			@DecimalMin(value = "0", message = "La propina no puede ser negativa.") BigDecimal propina,
			BigDecimal recibido) {
	}
}
