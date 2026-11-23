package ar.viaveneto.carta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Categoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 60)
	private String nombre;

	/** Posición en la que aparece en la pantalla de pedidos. */
	@Column(nullable = false)
	private int orden;

	public Categoria(String nombre, int orden) {
		this.nombre = nombre;
		this.orden = orden;
	}
}
