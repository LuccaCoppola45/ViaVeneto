package ar.viaveneto.acceso;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un dispositivo con alguien adentro. El token viaja en cada llamada;
 * acá solo se guarda su hash, así una copia de la base no sirve para entrar.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sesion {

	@Id
	@Column(length = 64)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	private Usuario usuario;

	@Column(nullable = false)
	private LocalDateTime creada;

	@Column(nullable = false)
	private LocalDateTime vence;

	Sesion(String tokenHash, Usuario usuario, LocalDateTime ahora, LocalDateTime vence) {
		this.tokenHash = tokenHash;
		this.usuario = usuario;
		this.creada = ahora;
		this.vence = vence;
	}

	boolean vigente(LocalDateTime ahora) {
		return ahora.isBefore(vence) && usuario.isActivo();
	}

	/** Mientras se use, la sesión no vence: la tablet queda logueada. */
	void extenderHasta(LocalDateTime vence) {
		this.vence = vence;
	}
}
