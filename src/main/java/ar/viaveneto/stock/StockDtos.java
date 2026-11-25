package ar.viaveneto.stock;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class StockDtos {

	private StockDtos() {
	}

	public record InsumoVista(Long id, String nombre, Unidad unidad, BigDecimal cantidad, BigDecimal minimo,
			EstadoStock estado, List<String> usadoEn) {
	}

	public record LineaReceta(Long insumoId, String insumo, Unidad unidad, BigDecimal cantidad) {
	}

	public record RecetaVista(Long productoId, String producto, String categoria, boolean activo, List<LineaReceta> lineas) {
	}

	public record Alertas(int bajos, int sinStock) {
	}

	public record StockVista(List<InsumoVista> insumos, List<RecetaVista> recetas, Alertas alertas) {
	}

	public record InsumoForm(
			@NotBlank(message = "Poné el nombre.") @Size(max = 60, message = "El nombre es muy largo (máximo 60 letras).") String nombre,
			@NotNull(message = "Elegí cómo se cuenta.") Unidad unidad,
			@DecimalMin(value = "0", message = "La cantidad no puede ser negativa.") BigDecimal cantidad,
			@NotNull(message = "Poné el mínimo (puede ser 0).") @DecimalMin(value = "0", message = "El mínimo no puede ser negativo.") BigDecimal minimo) {
	}

	public record CantidadForm(
			@NotNull(message = "Poné la cantidad.") @DecimalMin(value = "0", message = "La cantidad no puede ser negativa.") BigDecimal cantidad) {
	}

	public record LineaForm(
			@NotNull(message = "Elegí el insumo.") Long insumoId,
			@NotNull(message = "Poné cuánto usa.") @DecimalMin(value = "0.001", message = "Poné cuánto usa.") BigDecimal cantidad) {
	}

	public record RecetaForm(@NotNull List<@Valid LineaForm> lineas) {
	}
}
