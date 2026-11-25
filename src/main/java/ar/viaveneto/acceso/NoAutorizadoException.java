package ar.viaveneto.acceso;

public class NoAutorizadoException extends RuntimeException {
	public NoAutorizadoException(String mensaje) {
		super(mensaje);
	}
}
