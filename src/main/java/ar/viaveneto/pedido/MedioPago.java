package ar.viaveneto.pedido;

public enum MedioPago {
	EFECTIVO, DEBITO, CREDITO, TRANSFERENCIA,
	/** Lo cobra Rappi: no pasa por el cajón. */
	RAPPI
}
