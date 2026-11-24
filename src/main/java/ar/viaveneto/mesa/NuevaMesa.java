package ar.viaveneto.mesa;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Si no viene el sector, la mesa va al primero. Se ubica sola en un lugar libre del plano. */
public record NuevaMesa(
		@Min(value = 1, message = "El número de mesa tiene que ser 1 o más.")
		@Max(value = 999, message = "El número de mesa es demasiado grande.")
		int numero,
		Long sectorId) {
}
