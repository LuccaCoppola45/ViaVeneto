package ar.viaveneto.salon;

/**
 * Medidas del plano en unidades propias: cada sector es un rectángulo de 1000 x 800
 * y el frontend lo escala al tamaño de la pantalla.
 */
public final class Plano {

	public static final int ANCHO = 1000;
	public static final int ALTO = 800;

	/**
	 * Cada mesa se arma con mesitas individuales de 2 lugares (una silla de cada lado) puestas en fila.
	 * Lo habitual son 2 mesitas: una mesa de 4.
	 */
	public static final int MESITA_LARGO = 90;
	public static final int MESA_CORTO = 120;
	public static final int MESITAS_HABITUALES = 2;
	public static final int MAXIMO_MESITAS = 6;
	public static final int MESA_LARGO = MESITA_LARGO * MESITAS_HABITUALES;

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
