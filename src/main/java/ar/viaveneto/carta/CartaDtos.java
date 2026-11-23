package ar.viaveneto.carta;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CartaDtos {

	private CartaDtos() {
	}

	public record ProductoVista(Long id, String nombre, BigDecimal precio, Long categoriaId, boolean activo) {
		static ProductoVista de(Producto p) {
			return new ProductoVista(p.getId(), p.getNombre(), p.getPrecio(), p.getCategoria().getId(), p.isActivo());
		}
	}

	public record CategoriaVista(Long id, String nombre, int orden, List<ProductoVista> productos) {
	}

	public record ProductoForm(
			@NotBlank(message = "Poné el nombre del producto.")
			@Size(max = 80, message = "El nombre es muy largo (máximo 80 letras).")
			String nombre,
			@NotNull(message = "Poné el precio.")
			@DecimalMin(value = "0", message = "El precio no puede ser negativo.")
			BigDecimal precio,
			@NotNull(message = "Elegí una categoría.")
			Long categoriaId,
			boolean activo) {
	}

	public record CategoriaForm(
			@NotBlank(message = "Poné el nombre de la categoría.")
			@Size(max = 60, message = "El nombre es muy largo (máximo 60 letras).")
			String nombre) {
	}
}
