package ar.viaveneto.caja;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import ar.viaveneto.pedido.MedioPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CajaDtos {

	private CajaDtos() {
	}

	/** Lo cobrado con un medio de pago. La propina va aparte porque no es venta. */
	public record PorMedio(MedioPago medio, int cobros, BigDecimal monto, BigDecimal propina) {
	}

	public record Resumen(
			List<PorMedio> porMedio,
			BigDecimal ventas,
			BigDecimal propinas,
			BigDecimal descuentos,
			int mesasCobradas) {
	}

	public record TurnoVista(
			Long id,
			TipoTurno tipo,
			String responsable,
			boolean automatico,
			LocalDateTime abiertoEn,
			LocalDateTime cerradoEn,
			BigDecimal fondoInicial,
			Resumen resumen,
			/** Cambio inicial + ventas en efectivo: lo que tendría que haber en el cajón. */
			BigDecimal efectivoEsperado,
			BigDecimal efectivoContado,
			/** Contado menos esperado: positivo sobra, negativo falta. */
			BigDecimal diferencia,
			String observaciones) {
	}

	/** Lo que propone la pantalla al abrir un turno. */
	public record Sugerencia(TipoTurno tipo, BigDecimal fondoInicial) {
	}

	public record CajaVista(TurnoVista turnoAbierto, List<TurnoVista> turnosDeHoy, Resumen dia, Sugerencia sugerencia) {
	}

	public record AbrirTurno(
			@NotNull(message = "Elegí el turno.") TipoTurno tipo,
			@NotBlank(message = "Poné quién está en la caja.")
			@Size(max = 40, message = "El nombre es muy largo.") String responsable,
			@NotNull(message = "Poné el cambio que hay en el cajón (puede ser 0).")
			@DecimalMin(value = "0", message = "El cambio no puede ser negativo.") BigDecimal fondoInicial) {
	}

	public record CerrarTurno(
			@NotNull(message = "Poné cuánta plata en efectivo contaste.")
			@DecimalMin(value = "0", message = "El efectivo no puede ser negativo.") BigDecimal efectivoContado,
			@Size(max = 300, message = "La nota es muy larga (máximo 300 letras).") String observaciones) {
	}
}
