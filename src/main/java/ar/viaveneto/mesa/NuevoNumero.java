package ar.viaveneto.mesa;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record NuevoNumero(
		@Min(value = 1, message = "El número de mesa tiene que ser 1 o más.")
		@Max(value = 999, message = "El número de mesa es demasiado grande.")
		int numero) {
}
