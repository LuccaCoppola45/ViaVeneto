package ar.viaveneto.acceso;

/** Quién está usando el sistema en este pedido HTTP. */
public record UsuarioActual(Long id, String nombre) {

	public static final String ATRIBUTO = "viaveneto.usuario";
}
