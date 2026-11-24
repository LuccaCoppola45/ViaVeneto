package ar.viaveneto.mesa;

import ar.viaveneto.salon.Plano;
import ar.viaveneto.salon.Sector;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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

	@ManyToOne(fetch = FetchType.LAZY)
	private Sector sector;

	/** Esquina de arriba a la izquierda en el plano del sector (ver {@link Plano}). */
	private int x;

	private int y;

	/** false: los lados largos quedan arriba y abajo; true: a los costados. */
	private boolean vertical;

	public Mesa(int numero, Sector sector, int x, int y) {
		this.numero = numero;
		ubicar(sector, x, y, false);
	}

	public void ubicar(Sector sector, int x, int y, boolean vertical) {
		this.sector = sector;
		this.vertical = vertical;
		this.x = Plano.limitarX(x, ancho());
		this.y = Plano.limitarY(y, alto());
	}

	public int ancho() {
		return vertical ? Plano.MESA_CORTO : Plano.MESA_LARGO;
	}

	public int alto() {
		return vertical ? Plano.MESA_LARGO : Plano.MESA_CORTO;
	}

	public void cambiarNumero(int numero) {
		this.numero = numero;
	}

	public void desactivar() {
		this.activa = false;
	}

	public void reactivar() {
		this.activa = true;
	}
}
