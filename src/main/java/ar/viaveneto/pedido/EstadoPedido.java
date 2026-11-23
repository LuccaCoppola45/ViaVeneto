package ar.viaveneto.pedido;

public enum EstadoPedido {
	ABIERTO,
	CERRADO,
	/** Se abrió y se cerró sin cargar nada (ej. tocaron la mesa equivocada). */
	ANULADO
}
