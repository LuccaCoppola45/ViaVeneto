package ar.viaveneto.acceso;

/** Quién está usando el sistema en este pedido HTTP. */
public record UsuarioActual(Long id, String nombre) {

	static final String ATRIBUTO = "viaveneto.usuario";
}
