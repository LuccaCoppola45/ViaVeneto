package ar.viaveneto.salon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElementoPlano {

	static final int MINIMO = 20;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Sector sector;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoElemento tipo;

	@Column(nullable = false)
	private int x;

	@Column(nullable = false)
	private int y;

	@Column(nullable = false)
	private int ancho;

	@Column(nullable = false)
	private int alto;

	public ElementoPlano(Sector sector, TipoElemento tipo, int x, int y, int ancho, int alto) {
		this.sector = sector;
		this.tipo = tipo;
		this.ancho = Math.clamp(ancho, MINIMO, Plano.ANCHO);
		this.alto = Math.clamp(alto, MINIMO, Plano.ALTO);
		this.x = Plano.limitarX(x, this.ancho);
		this.y = Plano.limitarY(y, this.alto);
	}
}
