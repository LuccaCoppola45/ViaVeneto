package ar.viaveneto.caja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ar.viaveneto.comun.ReglaNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un turno de caja: el mediodía (la abuela) o la noche (la mamá).
 * Se abre con el cambio que hay en el cajón y se cierra contando el efectivo.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Turno {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TipoTurno tipo;

	@Column(nullable = false, length = 40)
	private String responsable;

	/** El cambio que había en el cajón al abrir. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal fondoInicial;

	@Column(nullable = false)
	private LocalDateTime abiertoEn;

	/** true si se abrió solo porque alguien cobró sin abrir la caja. */
	@Column(nullable = false)
	private boolean automatico;

	private LocalDateTime cerradoEn;

	/** Los billetes que se contaron al cerrar. */
	@Column(precision = 12, scale = 2)
	private BigDecimal efectivoContado;

	@Column(length = 300)
	private String observaciones;

	public Turno(TipoTurno tipo, String responsable, BigDecimal fondoInicial, LocalDateTime ahora, boolean automatico) {
		this.tipo = tipo;
		this.responsable = responsable.trim();
		this.fondoInicial = fondoInicial;
		this.abiertoEn = ahora;
		this.automatico = automatico;
	}

	public boolean abierto() {
		return cerradoEn == null;
	}

	public void cerrar(BigDecimal efectivoContado, String observaciones, LocalDateTime ahora) {
		if (!abierto()) {
			throw new ReglaNegocioException("Este turno ya está cerrado.");
		}
		this.efectivoContado = efectivoContado;
		this.observaciones = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
		this.cerradoEn = ahora;
	}
}
