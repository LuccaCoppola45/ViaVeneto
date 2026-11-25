package ar.viaveneto.acceso;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Una persona que usa el sistema. Entra tocando su nombre y poniendo su PIN. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

	static final int INTENTOS_ANTES_DE_BLOQUEAR = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String nombre;

	/** El PIN nunca se guarda tal cual: solo su hash BCrypt. */
	@Column(nullable = false, length = 100)
	private String pinHash;

	@Column(nullable = false)
	private boolean activo = true;

	@Column(nullable = false)
	@ColumnDefault("0")
	private int intentosFallidos;

	/** Después de varios PIN mal puestos se bloquea un rato, para que no se pueda adivinar probando. */
	private LocalDateTime bloqueadoHasta;

	Usuario(String nombre, String pinHash) {
		this.nombre = nombre.trim();
		this.pinHash = pinHash;
	}

	boolean bloqueado(LocalDateTime ahora) {
		return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
	}

	void registrarFallo(LocalDateTime ahora, int segundosDeBloqueo) {
		intentosFallidos++;
		if (intentosFallidos >= INTENTOS_ANTES_DE_BLOQUEAR) {
			bloqueadoHasta = ahora.plusSeconds(segundosDeBloqueo);
			intentosFallidos = 0;
		}
	}

	void registrarIngreso() {
		intentosFallidos = 0;
		bloqueadoHasta = null;
	}

	void cambiarPin(String pinHash) {
		this.pinHash = pinHash;
		registrarIngreso();
	}

	void desactivar() {
		this.activo = false;
	}

	void reactivar(String pinHash) {
		this.activo = true;
		cambiarPin(pinHash);
	}
}
