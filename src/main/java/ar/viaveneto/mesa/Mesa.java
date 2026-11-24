package ar.viaveneto.mesa;

import org.hibernate.annotations.ColumnDefault;

import ar.viaveneto.salon.Plano;
import ar.viaveneto.salon.Sector;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_mesa_numero_sufijo", columnNames = { "numero", "sufijo" }))
@Getter
@NoArgsConstructor
public class Mesa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private int numero;

	/** "B", "C"… cuando una mesa se partió en dos (la 4 y la 4B). Vacío en las mesas normales. */
	@Column(nullable = false, length = 2)
	@ColumnDefault("''")
	private String sufijo = "";

	/** Cuántas mesitas individuales de 2 lugares la forman. */
	@Column(nullable = false)
	@ColumnDefault("2")
	private int mesitas = Plano.MESITAS_HABITUALES;

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
		this(numero, "", Plano.MESITAS_HABITUALES, sector, x, y, false);
	}

	Mesa(int numero, String sufijo, int mesitas, Sector sector, int x, int y, boolean vertical) {
		this.numero = numero;
		this.sufijo = sufijo;
		this.mesitas = mesitas;
		ubicar(sector, x, y, vertical);
	}

	/** Cómo se la nombra en pantalla: "4" o "4B". */
	public String etiqueta() {
		return numero + sufijo;
	}

	public int lugares() {
		return mesitas * 2;
	}

	public void ubicar(Sector sector, int x, int y, boolean vertical) {
		this.sector = sector;
		this.vertical = vertical;
		this.x = Plano.limitarX(x, ancho());
		this.y = Plano.limitarY(y, alto());
	}

	public int ancho() {
		return vertical ? Plano.MESA_CORTO : largo();
	}

	public int alto() {
		return vertical ? largo() : Plano.MESA_CORTO;
	}

	private int largo() {
		return Plano.MESITA_LARGO * mesitas;
	}

	void sumarMesitas(int cantidad) {
		this.mesitas += cantidad;
		ubicar(sector, x, y, vertical);
	}

	public void cambiarNumero(int numero) {
		this.numero = numero;
	}

	public void desactivar() {
		this.activa = false;
	}

	/** Vuelve al salón como una mesa nueva: con sus mesitas y en el lugar indicado. */
	void reactivar(int mesitas, Sector sector, int x, int y, boolean vertical) {
		this.activa = true;
		this.mesitas = mesitas;
		ubicar(sector, x, y, vertical);
	}
}
