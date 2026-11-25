package ar.viaveneto.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Montos en pesos: se redondean al peso y se muestran como "$ 18.000". */
public final class Plata {

	private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");

	private Plata() {
	}

	public static BigDecimal redondear(BigDecimal monto) {
		return monto.setScale(0, RoundingMode.HALF_UP);
	}

	public static BigDecimal porcentaje(BigDecimal monto, int porcentaje) {
		return redondear(monto.multiply(BigDecimal.valueOf(porcentaje)).divide(BigDecimal.valueOf(100)));
	}

	public static String formato(BigDecimal monto) {
		NumberFormat formato = NumberFormat.getIntegerInstance(ARGENTINA);
		return "$ " + formato.format(redondear(monto));
	}
}
