package ar.viaveneto.carta;

import java.math.BigDecimal;

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
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 80)
	private String nombre;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal precio;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Categoria categoria;

	/** Los productos que se dejan de vender se desactivan: siguen apareciendo en pedidos viejos. */
	@Column(nullable = false)
	private boolean activo = true;

	public Producto(String nombre, BigDecimal precio, Categoria categoria) {
		actualizar(nombre, precio, categoria, true);
	}

	public void actualizar(String nombre, BigDecimal precio, Categoria categoria, boolean activo) {
		this.nombre = nombre.trim();
		this.precio = precio;
		this.categoria = categoria;
		this.activo = activo;
	}
}
