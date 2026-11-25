package ar.viaveneto.acceso;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AccesoDtos {

	static final String PIN = "\\d{4}";
	static final String MENSAJE_PIN = "El PIN tiene que ser de 4 números.";

	private AccesoDtos() {
	}

	public record UsuarioVista(Long id, String nombre) {
		static UsuarioVista de(Usuario u) {
			return new UsuarioVista(u.getId(), u.getNombre());
		}
	}

	/** Lo que necesita la pantalla de ingreso: los nombres para tocar, o que hay que crear el primero. */
	public record Estado(boolean hayUsuarios, List<UsuarioVista> usuarios) {
	}

	public record Ingreso(@NotNull(message = "Elegí quién sos.") Long usuarioId,
			@NotNull(message = MENSAJE_PIN) @Pattern(regexp = PIN, message = MENSAJE_PIN) String pin) {
	}

	public record SesionVista(String token, UsuarioVista usuario) {
	}

	public record NuevoUsuario(
			@NotBlank(message = "Poné el nombre.") @Size(max = 30, message = "El nombre es muy largo (máximo 30 letras).") String nombre,
			@NotNull(message = MENSAJE_PIN) @Pattern(regexp = PIN, message = MENSAJE_PIN) String pin) {
	}

	public record NuevoPin(@NotNull(message = MENSAJE_PIN) @Pattern(regexp = PIN, message = MENSAJE_PIN) String pin) {
	}
}
