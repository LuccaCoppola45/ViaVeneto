package ar.viaveneto.salon;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SalonDtos {

	private SalonDtos() {
	}

	public record ElementoVista(TipoElemento tipo, int x, int y, int ancho, int alto) {
		static ElementoVista de(ElementoPlano e) {
			return new ElementoVista(e.getTipo(), e.getX(), e.getY(), e.getAncho(), e.getAlto());
		}
	}

	public record SectorVista(Long id, String nombre, int orden, List<ElementoVista> elementos) {
	}

	/** Medidas que usa el frontend para dibujar, así no quedan repetidas en los dos lados. */
	public record Medidas(int ancho, int alto, int mesitaLargo, int mesaCorto) {
	}

	public record SalonVista(Medidas medidas, List<SectorVista> sectores) {
	}

	public record UbicacionMesa(@NotNull Long id, @NotNull Long sectorId, int x, int y, boolean vertical) {
	}

	public record ElementoForm(@NotNull Long sectorId, @NotNull TipoElemento tipo, int x, int y, int ancho, int alto) {
	}

	/** Todo lo acomodado de una vez: las mesas que se movieron y todos los elementos del local. */
	public record PlanoForm(@NotNull List<@Valid UbicacionMesa> mesas, @NotNull List<@Valid ElementoForm> elementos) {
	}

	public record SectorForm(
			@NotBlank(message = "Poné el nombre del sector.")
			@Size(max = 40, message = "El nombre es muy largo (máximo 40 letras).")
			String nombre) {
	}
}
