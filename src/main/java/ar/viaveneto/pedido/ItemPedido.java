package ar.viaveneto.pedido;

import java.math.BigDecimal;

import ar.viaveneto.carta.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class ItemPedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Pedido pedido;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Producto producto;

	/** Nombre y precio se copian al cargar: si después cambia la carta, el pedido no se modifica. */
	@Column(nullable = false, length = 80)
	private String nombre;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal precioUnitario;

	@Column(nullable = false)
	private int cantidad;

	ItemPedido(Pedido pedido, Producto producto, int cantidad) {
		this.pedido = pedido;
		this.producto = producto;
		this.nombre = producto.getNombre();
		this.precioUnitario = producto.getPrecio();
		this.cantidad = cantidad;
	}

	void sumar(int unidades) {
		this.cantidad += unidades;
	}

	void cambiarCantidad(int cantidad) {
		this.cantidad = cantidad;
	}

	public BigDecimal subtotal() {
		return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
	}
}
