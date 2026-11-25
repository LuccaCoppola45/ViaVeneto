package ar.viaveneto.pedido;

/** Para los pedidos sin mesa: en qué anda el pedido en la cocina y el mostrador. */
public enum EstadoEntrega {
	PREPARANDO,
	/** Listo para que lo pasen a buscar (el cliente o el repartidor de Rappi). */
	LISTO,
	ENTREGADO
}
