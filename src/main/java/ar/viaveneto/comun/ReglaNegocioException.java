package ar.viaveneto.comun;

/** Lo que el usuario pidió no se puede hacer en este momento (ej. abrir una mesa ya ocupada). */
public class ReglaNegocioException extends RuntimeException {
	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}
}
