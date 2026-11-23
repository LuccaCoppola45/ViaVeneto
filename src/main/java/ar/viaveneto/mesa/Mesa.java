package ar.viaveneto.mesa;

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
public class Mesa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private int numero;

	/** Las mesas que se sacan del salón quedan inactivas para no perder sus pedidos viejos. */
	@Column(nullable = false)
	private boolean activa = true;

	public Mesa(int numero) {
		this.numero = numero;
	}

	public void desactivar() {
		this.activa = false;
	}

	public void reactivar() {
		this.activa = true;
	}
}
