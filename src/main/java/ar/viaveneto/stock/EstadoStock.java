package ar.viaveneto.stock;

public enum EstadoStock {
	OK,
	/** Llegó al mínimo: hay que comprar. */
	BAJO,
	SIN_STOCK
}
