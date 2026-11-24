package ar.viaveneto.salon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Una parte del local con su propio plano: planta baja, arriba, cerramiento, afuera. */
@Entity
@Getter
@NoArgsConstructor
public class Sector {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 40)
	private String nombre;

	@Column(nullable = false)
	private int orden;

	public Sector(String nombre, int orden) {
		this.nombre = nombre;
		this.orden = orden;
	}
}
