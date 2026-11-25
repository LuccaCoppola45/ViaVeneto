package ar.viaveneto.stock;

import java.math.BigDecimal;

import ar.viaveneto.carta.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Cuánto de un insumo gasta cada unidad vendida de un plato: 1 milanesa = 0,25 kg de papas. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_receta_producto_insumo", columnNames = { "producto_id", "insumo_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecetaItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Producto producto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Insumo insumo;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidad;

	public RecetaItem(Producto producto, Insumo insumo, BigDecimal cantidad) {
		this.producto = producto;
		this.insumo = insumo;
		this.cantidad = cantidad;
	}

	BigDecimal para(int unidades) {
		return cantidad.multiply(BigDecimal.valueOf(unidades));
	}
}
