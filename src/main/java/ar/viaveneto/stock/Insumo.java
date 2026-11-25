package ar.viaveneto.stock;

import java.math.BigDecimal;

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
 * Algo que se compra y se gasta: una lata de gaseosa, kilos de papas, litros de vino.
 * Los platos de la carta descuentan insumos según su receta.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Insumo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 60)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Unidad unidad;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidad;

	/** Con esta cantidad o menos aparece el aviso de "hay que comprar". */
	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal minimo;

	@Column(nullable = false)
	private boolean activo = true;

	public Insumo(String nombre, Unidad unidad, BigDecimal cantidad, BigDecimal minimo) {
		editar(nombre, unidad, minimo);
		this.cantidad = cantidad;
	}

	public void editar(String nombre, Unidad unidad, BigDecimal minimo) {
		this.nombre = nombre.trim();
		this.unidad = unidad;
		this.minimo = minimo;
	}

	public EstadoStock estado() {
		if (cantidad.signum() <= 0) {
			return EstadoStock.SIN_STOCK;
		}
		return cantidad.compareTo(minimo) <= 0 ? EstadoStock.BAJO : EstadoStock.OK;
	}

	public boolean alcanza(BigDecimal necesario) {
		return cantidad.compareTo(necesario) >= 0;
	}

	void sumar(BigDecimal diferencia) {
		this.cantidad = cantidad.add(diferencia);
	}

	/** Después de contar lo que hay de verdad en el depósito. */
	void contar(BigDecimal cantidadReal) {
		this.cantidad = cantidadReal;
	}

	void desactivar() {
		this.activo = false;
	}

	void reactivar(Unidad unidad, BigDecimal cantidad, BigDecimal minimo) {
		this.activo = true;
		this.unidad = unidad;
		this.cantidad = cantidad;
		this.minimo = minimo;
	}
}
