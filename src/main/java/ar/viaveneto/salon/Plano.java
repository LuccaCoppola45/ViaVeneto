package ar.viaveneto.salon;

/**
 * Medidas del plano en unidades propias: cada sector es un rectángulo de 1000 x 625
 * y el frontend lo escala al tamaño de la pantalla.
 */
public final class Plano {

	public static final int ANCHO = 1000;
	public static final int ALTO = 625;

	/** Las mesas son de 4 lugares (dos mesitas juntas): rectángulo con sillas en los lados largos. */
	public static final int MESA_LARGO = 180;
	public static final int MESA_CORTO = 120;

	private Plano() {
	}

	/** Deja la pieza adentro del plano aunque la manden corrida. */
	public static int limitarX(int x, int ancho) {
		return Math.clamp(x, 0, ANCHO - ancho);
	}

	public static int limitarY(int y, int alto) {
		return Math.clamp(y, 0, ALTO - alto);
	}
}
